"""Synthetic tests for the implement-issue harness; no live agents or builds."""

from __future__ import annotations

import json
import io
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest
from types import SimpleNamespace
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parent))
import run  # noqa: E402

REPO = Path(__file__).resolve().parent.parent
ISSUE_35 = REPO / "evals/implement-issues/cases/member-status-view.yaml"


def put(root: Path, name: str, text: str) -> Path:
    path = root / name
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")
    return path


def junit_xml(tests: int = 1, failures: int = 0, errors: int = 0, skipped: int = 0) -> str:
    cases = []
    for index in range(tests):
        if index < failures:
            outcome = "<failure/>"
        elif index < failures + errors:
            outcome = "<error/>"
        elif index < failures + errors + skipped:
            outcome = "<skipped/>"
        else:
            outcome = ""
        cases.append(f'<testcase name="test-{index}">{outcome}</testcase>')
    return (f'<testsuite tests="{tests}" failures="{failures}" errors="{errors}" '
            f'skipped="{skipped}">{"".join(cases)}</testsuite>')


def commit(root: Path) -> None:
    for command in (["git", "init", "-q"], ["git", "config", "user.name", "Fixture"],
                    ["git", "config", "user.email", "fixture@example.invalid"],
                    ["git", "add", "-A"], ["git", "commit", "-qm", "Fixture baseline"]):
        subprocess.run(command, cwd=root, check=True)


def workspace(root: Path) -> Path:
    root.mkdir(parents=True, exist_ok=True)
    files = {
        ".gitignore": "build/\n",
        "build.gradle": "plugins {}\n",
        "settings.gradle": "rootProject.name = 'fixture'\n",
        "gradle/wrapper/gradle-wrapper.jar": "fixture\n",
        "gradle/wrapper/gradle-wrapper.properties": "distributionUrl=fixture\n",
        "src/main/java/gymmie/Router.java": "showDashboard()\n",
        "src/main/java/gymmie/LoginController.java": "showDashboard()\n",
        "src/main/java/gymmie/persistence/repository/MembershipRepository.java":
            "findByMemberId(Connection connection, long memberId)\n",
        "src/main/java/gymmie/persistence/Persistence.java": "MembershipRepository memberships\n",
        "src/main/java/gymmie/service/Permissions.java":
            "Account requireOwner(Connection connection, Role role, long ownerAccountId)\n",
        "src/test/java/gymmie/model/MemberTest.java": "evaluator-owned history tests\n",
        "src/test/java/gymmie/model/MembershipTest.java": "evaluator-owned date tests\n",
        "src/main/java/gymmie/Existing.java": "baseline\n",
        "valuable.txt": "pre-existing user file\n",
    }
    for name, text in files.items():
        put(root, name, text)
    wrapper = put(root, "gradlew", f"#!{sys.executable}\n" +
                  "import pathlib, sys\n" +
                  "if pathlib.Path('fail-gradle').exists(): sys.exit(9)\n" +
                  "root=pathlib.Path('build/test-results/test')\n" +
                  "root.mkdir(parents=True, exist_ok=True)\n" +
                  "names=['TEST-fixture.xml'] if '--tests' not in sys.argv else "
                  "['TEST-gymmie.model.MemberTest.xml','TEST-gymmie.model.MembershipTest.xml']\n" +
                  "[ (root/name).write_text('<testsuite tests=\"1\" failures=\"0\" errors=\"0\" skipped=\"0\"><testcase name=\"test\"/></testsuite>') for name in names ]\n")
    wrapper.chmod(0o755)
    put(root, ".agents/skills/implement-issue/SKILL.md", "# Workflow\n")
    put(root, ".agents/skills/implement-issue/references/workflow-contract.md", "# Contract\n")
    commit(root)
    return root


def fake_codex(bin_dir: Path) -> None:
    script = f"#!{sys.executable}\n" + "import json, pathlib, sys\n" + \
        "a=sys.argv[1:]\n" + \
        "if a[:2] == ['login','status']: print('Logged in'); sys.exit(0)\n" + \
        "if a and a[0] == 'exec':\n" + \
        " prompt=sys.stdin.read()\n" + \
        " if a[-1] != '-': sys.exit(10)\n" + \
        " w=pathlib.Path(a[a.index('--cd')+1])\n" + \
        " output=pathlib.Path(a[a.index('--output-last-message')+1])\n" + \
        " if '--output-schema' in a:\n" + \
        "  output.with_suffix('.argv.json').write_text(json.dumps(a))\n" + \
        "  output.with_suffix('.cwd-existed').write_text(str(w.is_dir()))\n" + \
        "  payload={'judgments':[{'check_id':'feature_quality','status':'pass','evidence_refs':['diff.patch'],'explanation':'Stub assessment only.','uncertainty':'Synthetic.'}]}\n" + \
        "  output.write_text(json.dumps(payload))\n" + \
        " else:\n" + \
        "  if prompt.startswith('Add the Member-facing view'):\n" + \
        "   pathlib.Path(w/'src/main/java/gymmie/AgentChange.java').write_text('new work\\n')\n" + \
        "   output.write_text('Implemented the requested change.')\n" + \
        "   print(json.dumps({'type':'item.completed','item':{'type':'command_execution','command':'./gradlew test','exit_code':0,'status':'completed'}}))\n" + \
        "  else:\n" + \
        "   output.write_text('Plan switching is unsupported in v1.0; cancel then buy another plan.')\n" + \
        " print(json.dumps({'type':'turn.completed','usage':{'input_tokens':7,'output_tokens':3},'model':'stub-model'}))\n" + \
        " sys.exit(0)\n" + "sys.exit(8)\n"
    path = put(bin_dir, "codex", script)
    path.chmod(0o755)


def write_case(run_dir: Path, case: dict) -> None:
    import yaml
    (run_dir / "case.yaml").write_text(
        yaml.safe_dump({k: v for k, v in case.items() if not k.startswith("_")}, sort_keys=False), encoding="utf-8")


def metadata_for(run_dir: Path, target: Path, output: Path, case: dict) -> dict:
    return {"run_id": "fixture", "case_id": case["id"], "workspace": str(target), "output_dir": str(output),
            "workspace_fingerprint_after": run.workspace_fingerprint(target, output),
            "harness_fingerprint": run.sha256(Path(run.__file__).read_bytes()), "human_readiness": {},
            "starting_protected": {name: run.sha256((target / name).read_bytes()) for name in
                                   ("gradlew", "build.gradle", "settings.gradle", "gradle/wrapper/gradle-wrapper.jar",
                                   "gradle/wrapper/gradle-wrapper.properties", "src/test/java/gymmie/NavigationTest.java",
                                   "src/test/java/gymmie/model/MemberTest.java",
                                   "src/test/java/gymmie/model/MembershipTest.java")
                                   if (target / name).is_file()}}


class HarnessTests(unittest.TestCase):
    def setUp(self) -> None:
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)

    def tearDown(self) -> None:
        self.temp.cleanup()

    def test_dataset_and_string_human_checks_validate(self) -> None:
        self.assertEqual([], run.validate_dataset())
        case = run.load_yaml(ISSUE_35)
        self.assertIn("already implemented", case["preconditions"]["applicability"][0])
        human = [item for item in run.initial_checks(case) if item["kind"] == "human"]
        self.assertEqual(3, len(human))
        self.assertTrue(all(item["status"] == "pending" for item in human))
        broken = dict(case)
        broken["checks"] = {**case["checks"], "deterministic": {"items": [
            {"requirement_ids": ["missing"], "observation": "fixture"}]}}
        self.assertTrue(any("unknown requirement" in error for error in run.validate_case(broken, "broken.yaml")))
        answer_case = run.load_yaml(REPO / "evals/implement-issues/cases/membership-switch-scope-decision.yaml")
        self.assertEqual("semantic", next(item["kind"] for item in run.initial_checks(answer_case)
                                            if item["id"] == "answer_correctness"))

    def test_readiness_evidence_defers_when_a_prerequisite_is_missing(self) -> None:
        target = workspace(self.root / "target")
        case = run.load_yaml(ISSUE_35)
        initial = run.preflight(case, target, {})
        self.assertEqual("deferred", initial["status"])
        self.assertIn("issue_35_feature_already_complete", " ".join(initial["missing"]))
        (target / "src/main/java/gymmie/Router.java").unlink()
        result = run.preflight(case, target, {})
        self.assertEqual("deferred", result["status"])
        self.assertIn("Router.java is absent", " ".join(result["missing"]))

    def test_human_foundation_readiness_and_feature_applicability(self) -> None:
        target = workspace(self.root / "target")
        renewal = run.load_yaml(REPO / "evals/implement-issues/cases/member-renewal-boundaries.yaml")
        self.assertEqual("deferred", run.preflight(renewal, target, {})["status"])
        self.assertEqual("ready", run.preflight(renewal, target,
                                                  {"shared_foundation_ready": "Reviewed current service and routing foundation."})["status"])
        case = run.load_yaml(ISSUE_35)
        self.assertEqual("deferred", run.preflight(case, target, {})["status"])
        self.assertEqual("deferred", run.preflight(case, target,
                                                    {"issue_35_feature_already_complete": "Verified complete view."})["status"])
        self.assertEqual("ready", run.preflight(case, target,
                                                  {"issue_35_feature_already_complete": "incomplete: behavior is absent."})["status"])
        self.assertEqual("not_applicable", run.preflight(case, target,
                                                          {"issue_35_feature_already_complete": "complete: verified requested behavior."})["status"])
        guessed = target / "src/main/java/gymmie/MemberMembershipController.java"
        guessed.write_text("guessed filename does not determine applicability")
        self.assertEqual("deferred", run.preflight(case, target, {})["status"])

    def test_dirty_existing_workspace_is_refused_and_preserved(self) -> None:
        target = workspace(self.root / "target")
        case = run.find_case(REPO / "evals/implement-issues/cases", "gymmie-e4-035-member-status-view")
        valuable = target / "valuable.txt"
        valuable.write_text("user change\n")
        before = valuable.read_bytes()
        with self.assertRaisesRegex(ValueError, "clean relevant workspace"):
            run.run_case(case, target, self.root / "runs", 5, None, {})
        self.assertEqual(before, valuable.read_bytes())
        self.assertFalse((self.root / "runs").exists())

    def test_output_paths_cannot_mask_workspace_or_inputs(self) -> None:
        target = workspace(self.root / "workspace")
        unsafe = [target, target.parent, target / "src", target / "evals"]
        alias = self.root / "workspace-alias"
        alias.symlink_to(target, target_is_directory=True)
        source_alias = self.root / "source-alias"
        source_alias.symlink_to(target / "src", target_is_directory=True)
        unsafe.extend([alias, source_alias])
        for output in unsafe:
            with self.subTest(output=output), self.assertRaisesRegex(ValueError, "output directory"):
                run.validate_output_location(target, output)

    def test_dedicated_artifact_directories_preserve_freshness_and_dirty_checks(self) -> None:
        target = workspace(self.root / "artifact-workspace")
        output = target / ".evaluation-artifacts"
        resolved_workspace, resolved_output = run.validate_output_location(target, output)
        self.assertEqual(target.resolve(), resolved_workspace)
        self.assertEqual(output.resolve(), resolved_output)
        run_id = "20260925T000000Z-01234567"
        artifact = output / run_id
        artifact.mkdir(parents=True)
        run.json_write(artifact / "metadata.json", {"run_id": run_id, "case_id": "fixture-case",
                                                     "harness_fingerprint": "fixture",
                                                     "output_dir": str(output.resolve())})
        source = target / "src/main/java/gymmie/Existing.java"
        source.write_text("pre-existing edit")
        self.assertTrue(run.dirty_relevant(target, output))
        before = run.workspace_fingerprint(target, output)
        source.write_text("second edit")
        self.assertNotEqual(before, run.workspace_fingerprint(target, output))
        self.assertEqual([], [item for item in run.dirty_relevant(target, output)
                              if item["path"].startswith(".evaluation-artifacts/")])
        run_dir = self.root / "artifact-freshness-run"
        run_dir.mkdir()
        case = run.load_yaml(ISSUE_35)
        write_case(run_dir, case)
        run.json_write(run_dir / "metadata.json", metadata_for(run_dir, target, output, case))
        self.assertEqual("recorded", run.verify_run(run_dir)["status"])
        source.write_text("edit after artifact-backed verification")
        self.assertTrue(run.run_stale(run_dir))
        self.assertEqual("stale", run.verify_run(run_dir)["status"])

    def test_output_directory_refuses_unrecognized_existing_files(self) -> None:
        target = workspace(self.root / "uncurated-output-workspace")
        output = target / ".evaluation-artifacts"
        output.mkdir()
        (output / "valuable.txt").write_text("pre-existing user file")
        with self.assertRaisesRegex(ValueError, "non-harness content"):
            run.validate_output_location(target, output)
        self.assertEqual("pre-existing user file", (output / "valuable.txt").read_text())
        self.assertEqual(REPO.resolve(), run.validate_output_location(REPO, run.DEFAULT_RUNS)[0])

    def test_application_database_and_sqlite_sidecars_must_be_absent(self) -> None:
        target = workspace(self.root / "database-workspace")
        case = run.load_yaml(ISSUE_35)
        self.assertEqual("pass", run.application_data_readiness(target)["status"])
        data = target / "data"
        before = run.workspace_fingerprint(target, self.root / "runs")
        data.mkdir()
        db = data / "gymmie.db"
        db.write_bytes(b"fixture bytes; never opened")
        self.assertNotEqual(before, run.workspace_fingerprint(target, self.root / "runs"))
        blocked = run.preflight(case, target, {
            "issue_35_feature_already_complete": "incomplete: fixture"})
        self.assertEqual("deferred", blocked["status"])
        self.assertEqual("fail", next(item for item in blocked["checks"]
                                        if item["id"] == "application_data_disposable")["status"])
        db.unlink()
        sidecar = data / "gymmie.db-wal"
        sidecar.write_bytes(b"fixture sidecar")
        self.assertIn("gymmie.db-wal", run.application_data_readiness(target)["message"])
        sidecar.unlink()
        external_db = self.root / "external.db"
        external_db.write_bytes(b"fixture bytes")
        db.symlink_to(external_db)
        self.assertIn("aliases", run.application_data_readiness(target)["message"])

    def test_readiness_reports_supported_human_ids_and_disposable_data_requirement(self) -> None:
        target = workspace(self.root / "readiness-guidance")
        status_case = run.load_yaml(ISSUE_35)
        report = run.preflight(status_case, target, {})
        self.assertEqual("complete: EVIDENCE or incomplete: EVIDENCE",
                         report["accepted_human_readiness"]["issue_35_feature_already_complete"])
        self.assertIn("graphical_test_environment", report["accepted_human_readiness"])
        self.assertTrue(any(check["id"] == "application_data_disposable" for check in report["checks"]))
        renewal = run.load_yaml(REPO / "evals/implement-issues/cases/member-renewal-boundaries.yaml")
        self.assertIn("shared_foundation_ready", run.preflight(renewal, target, {})["accepted_human_readiness"])

    def test_readiness_cli_prints_actionable_ids(self) -> None:
        target = workspace(self.root / "readiness-cli")
        args = SimpleNamespace(cases_dir=REPO / "evals/implement-issues/cases",
                               case="gymmie-e4-035-member-status-view", workspace=target,
                               output_dir=self.root / "runs", human_check=[])
        with patch.object(run.shutil, "which", return_value="codex"), \
                patch.object(run, "codex_login", return_value=True), \
                patch("sys.stdout", new_callable=io.StringIO) as output:
            run.command_ready(args)
        self.assertIn("issue_35_feature_already_complete", output.getvalue())
        self.assertIn("graphical_test_environment", output.getvalue())
        self.assertIn("complete: EVIDENCE or incomplete: EVIDENCE", output.getvalue())

    def test_implementation_workspace_must_be_on_user_prepared_feature_branch(self) -> None:
        target = workspace(self.root / "branch")
        case = run.load_yaml(ISSUE_35)
        self.assertIsNotNone(run.branch_blocker(target, case))
        subprocess.run(["git", "checkout", "-qb", "feature-35-fixture"], cwd=target, check=True)
        self.assertIsNone(run.branch_blocker(target, case))

    def test_working_tree_content_fingerprint_detects_same_status_different_content(self) -> None:
        target = workspace(self.root / "fingerprint")
        file = target / "src/main/java/gymmie/Existing.java"
        file.write_text("first dirty value\n")
        before = run.workspace_fingerprint(target, self.root / "runs")
        file.write_text("second dirty value\n")
        after = run.workspace_fingerprint(target, self.root / "runs")
        self.assertNotEqual(before, after)

    def test_complete_synthetic_run_uses_explicit_worktree_and_archived_case(self) -> None:
        base = workspace(self.root / "base")
        target = self.root / "worktree"
        subprocess.run(["git", "worktree", "add", "-b", "fixture-pilot", str(target)], cwd=base,
                       check=True, capture_output=True)
        put(target, "evals/run.py", "uncommitted harness under evaluation")
        put(target, "evals/implement-issues/cases/member-status-view.yaml", "uncommitted case under evaluation")
        cases_dir = self.root / "cases"
        cases_dir.mkdir()
        live_case = cases_dir / ISSUE_35.name
        shutil.copy2(ISSUE_35, live_case)
        case = run.find_case(cases_dir, "gymmie-e4-035-member-status-view")
        bin_dir = self.root / "bin"
        fake_codex(bin_dir)
        with patch.dict(os.environ, {"PATH": str(bin_dir) + os.pathsep + os.environ["PATH"]}):
            verify = run.verify_run

            def verify_while_staged(staged, include_ui=True):
                self.assertFalse((self.root / "runs").exists())
                self.assertNotEqual(target.resolve(), staged.resolve())
                return verify(staged, include_ui)

            with patch.object(run, "verify_run", side_effect=verify_while_staged):
                run_dir = run.run_case(case, target, self.root / "runs", 5, None,
                                       {"issue_35_feature_already_complete": "incomplete: behavior not present."},
                                       grade_model=True)
        self.assertIsNotNone(run_dir)
        metadata = json.loads((run_dir / "metadata.json").read_text())
        self.assertEqual("completed", metadata["status"])
        self.assertEqual(str(target.resolve()), metadata["workspace"])
        self.assertEqual(str(target.resolve() / "data/gymmie.db"),
                         metadata["execution"]["application_data"]["default_database"])
        self.assertEqual("pass", metadata["execution"]["application_data"]["disposable_check"]["status"])
        self.assertTrue((run_dir / "events.jsonl").is_file())
        self.assertTrue((run_dir / "stderr.log").is_file())
        self.assertEqual("Implemented the requested change.", (run_dir / "final-response.txt").read_text())
        self.assertIn("AgentChange.java", (run_dir / "diff.patch").read_text())
        self.assertTrue((run_dir / "workflow-inputs.tar.gz").is_file())
        grade_result = json.loads((run_dir / "model-grade.json").read_text())
        self.assertEqual("validated", grade_result["status"])
        verified_report = json.loads((run_dir / "verification.json").read_text())
        self.assertEqual(verified_report["verification_id"],
                         grade_result["evidence_association"]["verification_id"])
        attempt_dir = next((run_dir / "model-grade-attempts").iterdir())
        grader_argv = json.loads((attempt_dir / "output.argv.json").read_text())
        self.assertIn("--skip-git-repo-check", grader_argv)
        self.assertEqual("read-only", grader_argv[grader_argv.index("--sandbox") + 1])
        self.assertIn("approval_policy=never", grader_argv)
        self.assertIn("--ephemeral", grader_argv)
        self.assertIn("--ignore-user-config", grader_argv)
        self.assertFalse(any("dangerously" in arg or "danger-full-access" in arg for arg in grader_argv))
        self.assertIn("sandbox_workspace_write.network_access=false", grader_argv)
        self.assertTrue(Path(grader_argv[grader_argv.index("--cd") + 1]).name.startswith("gymmie-model-grader-"))
        self.assertEqual("True", (attempt_dir / "output.cwd-existed").read_text())
        self.assertIn("--output-schema", grader_argv)
        live_case.write_text(live_case.read_text().replace("Show the current membership's plan name", "changed live case"))
        report = json.loads((run_dir / "verification.json").read_text())
        self.assertEqual("pass", report["commands"][0]["status"])
        self.assertEqual("passed", report["commands"][0]["test_status"])
        self.assertEqual("pass", report["commands"][1]["status"])
        self.assertEqual("skipped", report["commands"][2]["status"])
        self.assertEqual("pass", next(item for item in report["checks"] if item["id"] == "membership_history_model")["status"])
        self.assertEqual("pass", next(item for item in report["checks"] if item["id"] == "verification_invocation")["status"])
        self.assertEqual("not_observable", next(item for item in report["checks"] if item["id"] == "IW-06")["status"])
        self.assertEqual("pending", next(item for item in report["checks"] if item["id"] == "feature_quality")["status"])
        self.assertEqual("not_implemented", next(item for item in report["checks"] if item["id"] == "member_view_content")["support_status"])
        self.assertTrue(all(item["status"] == "pending" for item in report["checks"] if item["kind"] == "human"))
        self.assertIn("current membership's plan name", run.load_yaml(run_dir / "case.yaml")["prompt"])
        self.assertTrue((base / "valuable.txt").is_file() and (target / "valuable.txt").is_file())

    def test_explanation_case_records_repository_no_change_without_external_claim(self) -> None:
        target = workspace(self.root / "explanation")
        case = run.find_case(REPO / "evals/implement-issues/cases", "gymmie-e4-044-membership-switch-scope-decision")
        bin_dir = self.root / "bin"
        fake_codex(bin_dir)
        with patch.dict(os.environ, {"PATH": str(bin_dir) + os.pathsep + os.environ["PATH"]}):
            run_dir = run.run_case(case, target, self.root / "runs", 5, None, {})
        report = json.loads((run_dir / "verification.json").read_text())
        unchanged = next(item for item in report["checks"] if item["id"] == "repository_no_change")
        self.assertEqual("pass", unchanged["status"])
        self.assertIn("external issue state", unchanged["explanation"])
        self.assertEqual([], report["commands"])

    def test_process_exit_failures_and_timeouts_are_truthful(self) -> None:
        env = {"PATH": "/usr/bin:/bin"}
        failed = run.process_limited([sys.executable, "-c", "raise SystemExit(7)"], self.root, 3, env,
                                     self.root / "fail.out", self.root / "fail.err")
        timeout = run.process_limited([sys.executable, "-c", "import time; time.sleep(2)"], self.root, 1, env,
                                      self.root / "timeout.out", self.root / "timeout.err")
        self.assertEqual(7, failed["exit_code"])
        self.assertFalse(failed["timed_out"])
        self.assertTrue(timeout["timed_out"])
        self.assertNotEqual(0, timeout["exit_code"])

    def test_verification_environment_keeps_java_config_and_filters_codex_credentials(self) -> None:
        with patch.dict(os.environ, {"CODEX_API_KEY": "secret", "OPENAI_API_KEY": "secret",
                                     "JAVA_HOME": "/fixture/java", "GRADLE_USER_HOME": "/fixture/gradle",
                                     "DISPLAY": ":9"}):
            env = run.verification_env(self.root)
        self.assertNotIn("CODEX_API_KEY", env)
        self.assertNotIn("OPENAI_API_KEY", env)
        self.assertEqual("/fixture/java", env["JAVA_HOME"])
        self.assertEqual("/fixture/gradle", env["GRADLE_USER_HOME"])
        self.assertEqual(":9", env["DISPLAY"])
        self.assertNotEqual(str(Path.home()), env["HOME"])
        with patch.dict(os.environ, {"JAVA_HOME": "/fixture/java", "GRADLE_USER_HOME": "/fixture/gradle",
                                     "SYSTEMROOT": "/fixture/windows", "CODEX_API_KEY": "secret"}):
            agent_env = run.implementation_env(self.root)
        self.assertEqual("/fixture/java", agent_env["JAVA_HOME"])
        self.assertEqual("/fixture/gradle", agent_env["GRADLE_USER_HOME"])
        self.assertEqual("/fixture/windows", agent_env["SYSTEMROOT"])
        self.assertNotIn("CODEX_API_KEY", agent_env)
        with patch.dict(os.environ, {"PATH": "/fixture/bin"}, clear=True):
            default_cache = run.implementation_env(self.root)["GRADLE_USER_HOME"]
        self.assertEqual(str(Path.home() / ".gradle"), default_cache)
        with patch.dict(os.environ, {"PATH": "/fixture/bin"}, clear=True):
            default_cache = run.implementation_env(self.root)["GRADLE_USER_HOME"]
        self.assertEqual(str(Path.home() / ".gradle"), default_cache)

    def _verify_fixture(self, name: str) -> tuple[Path, Path, dict]:
        target = workspace(self.root / f"{name}-workspace")
        run_dir = self.root / name
        run_dir.mkdir()
        case = run.load_yaml(ISSUE_35)
        write_case(run_dir, case)
        output = self.root / "runs"
        run.json_write(run_dir / "metadata.json", metadata_for(run_dir, target, output, case))
        return target, run_dir, case

    def test_missing_gui_readiness_does_not_skip_repository_check(self) -> None:
        _target, run_dir, _case = self._verify_fixture("gui")
        report = run.verify_run(run_dir)
        self.assertEqual("pass", report["commands"][0]["status"])
        self.assertEqual("skipped", report["commands"][2]["status"])

    def test_issue35_acceptance_tests_are_blocked_if_evaluator_inputs_change(self) -> None:
        target, run_dir, _case = self._verify_fixture("protected-tests")
        (target / "src/test/java/gymmie/model/MemberTest.java").write_text("candidate-modified tests\n")
        metadata = json.loads((run_dir / "metadata.json").read_text())
        metadata["workspace_fingerprint_after"] = run.workspace_fingerprint(target, self.root / "runs")
        run.json_write(run_dir / "metadata.json", metadata)
        report = run.verify_run(run_dir)
        self.assertEqual("blocked", report["commands"][0]["status"])
        self.assertEqual("blocked", report["commands"][1]["status"])

    def test_modified_wrapper_blocks_ui_and_all_verification_subprocesses(self) -> None:
        target, run_dir, _case = self._verify_fixture("protected-wrapper-ui")
        metadata = json.loads((run_dir / "metadata.json").read_text())
        metadata["human_readiness"] = {"graphical_test_environment": "desktop fixture available"}
        (target / "gradlew").write_text("#!/bin/sh\nexit 0\n")
        metadata["workspace_fingerprint_after"] = run.workspace_fingerprint(target, self.root / "runs")
        run.json_write(run_dir / "metadata.json", metadata)
        with patch.object(run, "process_limited") as process:
            report = run.verify_run(run_dir)
        self.assertEqual(["blocked", "blocked", "blocked"],
                         [command["status"] for command in report["commands"]])
        process.assert_not_called()
        self.assertIn("outside Codex sandbox", report["commands"][2]["execution_boundary"])

    def test_opt_in_ui_status_distinguishes_skipped_and_unavailable(self) -> None:
        target = workspace(self.root / "ui-workspace")
        run_dir = self.root / "ui"
        run_dir.mkdir()
        script = f"import pathlib; p=pathlib.Path('build/test-results/test/TEST-Nav.xml'); p.parent.mkdir(parents=True, exist_ok=True); p.write_text({junit_xml(skipped=1)!r})"
        skipped = run.verify_command(target, run_dir, "ui-skipped", [sys.executable, "-c", script], 3,
                                     ["build/test-results/test/TEST-*.xml"], "ui")
        self.assertEqual("skipped", skipped["status"])
        self.assertEqual("skipped", skipped["test_status"])
        unavailable = run.verify_command(target, run_dir, "ui-unavailable",
                                         [sys.executable, "-c", "import sys; sys.stderr.write('Unable to open display'); sys.exit(1)"],
                                         3, [], "ui")
        self.assertEqual("unavailable", unavailable["status"])

    def test_skipped_stale_and_malformed_test_reports_are_not_successful_tests(self) -> None:
        target = workspace(self.root / "reports-workspace")
        run_dir = self.root / "reports"
        run_dir.mkdir()
        reports = ["build/test-results/test/TEST-fixture.xml"]
        skipped_path = target / reports[0]
        skipped_path.parent.mkdir(parents=True)
        skipped_path.write_text(junit_xml(2, skipped=2))
        old = 1_000_000_000
        os.utime(skipped_path, ns=(old, old))
        result = run.verify_command(target, run_dir, "stale-report", [sys.executable, "-c", "pass"], 3,
                                    reports, "repository")
        self.assertEqual("unavailable", result["status"])
        self.assertEqual("unavailable", result["test_status"])
        self.assertEqual([], result["reports"])

        script = "import pathlib, os; p=pathlib.Path('build/test-results/test/TEST-fixture.xml'); p.write_text('<testsuite'); os.utime(p, None)"
        malformed = run.verify_command(target, run_dir, "malformed", [sys.executable, "-c", script], 3,
                                       reports, "repository")
        self.assertEqual("fail", malformed["status"])
        self.assertEqual("failed", malformed["test_status"])
        self.assertTrue((run_dir / malformed["reports"][0]).is_file())

    def test_all_skipped_tests_do_not_count_as_executed_or_passed(self) -> None:
        target = workspace(self.root / "all-skipped-workspace")
        run_dir = self.root / "all-skipped"
        run_dir.mkdir()
        script = f"import pathlib; p=pathlib.Path('build/test-results/test/TEST-fixture.xml'); p.parent.mkdir(parents=True, exist_ok=True); p.write_text({junit_xml(3, skipped=3)!r})"
        result = run.verify_command(target, run_dir, "skipped", [sys.executable, "-c", script], 3,
                                    ["build/test-results/test/TEST-*.xml"], "repository")
        self.assertEqual("skipped", result["test_status"])
        self.assertEqual("skipped", result["status"])
        self.assertEqual(0, result["tests"]["executed"])
        self.assertEqual(3, result["tests"]["skipped"])
        failing_script = f"import pathlib; p=pathlib.Path('build/test-results/test/TEST-fixture.xml'); p.write_text({junit_xml(1, failures=1)!r})"
        failed = run.verify_command(target, run_dir, "failed-report", [sys.executable, "-c", failing_script], 3,
                                    ["build/test-results/test/TEST-*.xml"], "repository")
        self.assertEqual("fail", failed["status"])
        self.assertEqual("failed", failed["test_status"])

    def test_required_acceptance_suites_and_junit_counts_are_complete_and_consistent(self) -> None:
        target = workspace(self.root / "acceptance-reports")
        run_dir = self.root / "acceptance-run"
        run_dir.mkdir()
        required = ["TEST-One.xml", "TEST-Two.xml"]

        def verify_reports(name: str, reports: dict[str, str]) -> dict:
            payload = json.dumps(reports)
            script = ("import json,pathlib,sys; root=pathlib.Path('build/test-results/test'); "
                      "root.mkdir(parents=True,exist_ok=True); "
                      "[(root/name).write_text(value) for name,value in json.loads(sys.argv[1]).items()]")
            return run.verify_command(target, run_dir, name,
                                      [sys.executable, "-c", script, payload], 3,
                                      ["build/test-results/test/TEST-*.xml"], "acceptance",
                                      required_reports=required)

        missing = verify_reports("missing-suite", {required[0]: junit_xml()})
        self.assertEqual("unavailable", missing["status"])
        self.assertEqual([required[1]], missing["missing_required_reports"])

        negative = verify_reports("negative-count", {required[0]: '<testsuite tests="-1" failures="0" errors="0" skipped="0"/>',
                                                       required[1]: junit_xml()})
        self.assertEqual("fail", negative["status"])
        self.assertTrue(negative["report_errors"])

        inconsistent_xml = junit_xml(2).replace('tests="2"', 'tests="3"', 1)
        inconsistent = verify_reports("inconsistent-count", {required[0]: inconsistent_xml,
                                                               required[1]: junit_xml()})
        self.assertEqual("fail", inconsistent["status"])
        self.assertTrue(inconsistent["report_errors"])

        skipped = verify_reports("skipped-required", {required[0]: junit_xml(1, skipped=1),
                                                       required[1]: junit_xml(1, skipped=1)})
        self.assertEqual("skipped", skipped["status"])
        self.assertEqual("skipped", skipped["test_status"])
        self.assertEqual(0, skipped["tests"]["executed"])

        partially_skipped = verify_reports("partially-skipped-required", {
            required[0]: junit_xml(1, skipped=1), required[1]: junit_xml()})
        self.assertEqual("skipped", partially_skipped["status"])
        self.assertEqual([required[0]], partially_skipped["skipped_required_reports"])

        complete = verify_reports("complete-suites", {required[0]: junit_xml(), required[1]: junit_xml()})
        self.assertEqual("pass", complete["status"])
        self.assertEqual(2, complete["tests"]["executed"])

        non_junit = run.verify_command(target, run_dir, "non-junit", [sys.executable, "-c", "pass"],
                                       3, [], "custom")
        self.assertEqual("pass", non_junit["status"])
        self.assertEqual("unavailable", non_junit["test_status"])

    def test_report_copy_is_per_invocation(self) -> None:
        target = workspace(self.root / "copies-workspace")
        run_dir = self.root / "copies"
        run_dir.mkdir()
        script = "import pathlib, sys; n=int(sys.argv[1]); p=pathlib.Path('build/test-results/test/TEST-fixture.xml'); p.parent.mkdir(parents=True, exist_ok=True); cases=''.join(f'<testcase name=\"t{i}\"/>' for i in range(n)); p.write_text(f'<testsuite tests=\"{n}\" failures=\"0\" errors=\"0\" skipped=\"0\">{cases}</testsuite>')"
        pattern = ["build/test-results/test/TEST-*.xml"]
        first = run.verify_command(target, run_dir, "first", [sys.executable, "-c", script, "1"], 3, pattern, "repo")
        second = run.verify_command(target, run_dir, "second", [sys.executable, "-c", script, "2"], 3, pattern, "repo")
        self.assertEqual(1, first["tests"]["tests"])
        self.assertEqual(2, second["tests"]["tests"])
        self.assertNotEqual((run_dir / first["reports"][0]).read_text(), (run_dir / second["reports"][0]).read_text())

    def test_command_failure_timeout_and_unavailable_are_distinct(self) -> None:
        target = workspace(self.root / "status-workspace")
        run_dir = self.root / "status"
        run_dir.mkdir()
        failed = run.verify_command(target, run_dir, "failure", [sys.executable, "-c", "raise SystemExit(6)"], 3, [], "repo")
        timed = run.verify_command(target, run_dir, "timeout", [sys.executable, "-c", "import time; time.sleep(2)"], 1, [], "repo")
        missing = run.verify_command(target, run_dir, "missing", [str(self.root / "not-a-command")], 3, [], "repo")
        self.assertEqual(("fail", 6), (failed["status"], failed["exit_code"]))
        self.assertEqual("timeout", timed["status"])
        self.assertEqual(("unavailable", 127), (missing["status"], missing["exit_code"]))

    def test_workspace_edits_after_verification_invalidate_saved_evidence(self) -> None:
        target, run_dir, case = self._verify_fixture("edit-after")
        report = run.verify_run(run_dir)
        self.assertEqual("recorded", report["status"])
        with patch("sys.stdout", new_callable=io.StringIO) as output:
            run.command_inspect(SimpleNamespace(run=str(run_dir), output_dir=self.root / "runs"))
            current = json.loads(output.getvalue())
        self.assertEqual("current", current["run_freshness"]["status"])
        self.assertEqual("current", current["verification_freshness"]["status"])
        (target / "src/main/java/gymmie/Existing.java").write_text("edited after check\n")
        self.assertTrue(run.run_stale(run_dir))
        with patch("sys.stdout", new_callable=io.StringIO) as output:
            run.command_inspect(SimpleNamespace(run=str(run_dir), output_dir=self.root / "runs"))
            stale = json.loads(output.getvalue())
        self.assertEqual("stale", stale["run_freshness"]["status"])
        self.assertEqual("stale", stale["verification_freshness"]["status"])
        self.assertEqual("recorded", stale["verification.json"]["status"])
        self.assertEqual(["pass", "pass", "skipped"],
                         [command["status"] for command in stale["verification.json"]["commands"]])
        self.assertEqual("stale", run.verify_run(run_dir)["status"])
        with self.assertRaisesRegex(ValueError, "stale"):
            run.model_evidence(run_dir)

    def test_model_judgments_follow_verification_and_exact_assessed_evidence(self) -> None:
        target, run_dir, _case = self._verify_fixture("grading-freshness")
        run.verify_run(run_dir)
        (run_dir / "diff.patch").write_text("stable candidate diff")
        (run_dir / "final-response.txt").write_text("candidate response")
        (run_dir / "events.jsonl").write_text(json.dumps({"type": "item.completed", "item": {
            "type": "command_execution", "command": "./gradlew check", "exit_code": 0,
            "status": "completed"}}))

        def grader(_prompt, _schema, _run_dir):
            return {"judgments": [{"check_id": "feature_quality", "status": "pass",
                    "evidence_refs": ["diff.patch"], "explanation": "Synthetic evidence.",
                    "uncertainty": "Stubbed."}]}, {"status": "completed", "model_actual": "stub"}

        first = run.model_grade(run_dir, invoke=grader)
        first_path = run_dir / "model-grade.json"
        self.assertEqual("current", run.model_association_freshness(run_dir, first)["status"])
        self.assertEqual(first["evidence_association"], run.evidence_association(
            run_dir, run.model_evidence(run_dir)[0]))

        failed = {"status": "fail", "exit_code": 1, "timed_out": False, "reports": [],
                  "tests": {"executed": 0}, "report_errors": []}
        with patch.object(run, "verify_command", side_effect=[failed, failed]):
            second_report = run.verify_run(run_dir, include_ui=False)
        self.assertNotEqual(first["evidence_association"]["verification_id"],
                            second_report["verification_id"])
        self.assertEqual("stale", run.model_association_freshness(run_dir, first)["status"])

        captured = []
        with patch("sys.stdout", new_callable=io.StringIO) as output:
            run.command_inspect(SimpleNamespace(run=str(run_dir), output_dir=self.root / "runs"))
            captured.append(json.loads(output.getvalue()))
        inspected = captured[0]
        historical = inspected["model_gradings"][0]
        self.assertEqual("stale", historical["evidence_freshness"]["status"])
        self.assertIsNone(inspected["current_model_grading"])

        records = self.root / "stale-records"
        run.command_record(SimpleNamespace(run=str(run_dir), output_dir=self.root / "runs", records_dir=records))
        record = json.loads(next(records.glob("*.json")).read_text())
        self.assertNotIn("model_grading", record)
        self.assertEqual("stale_not_exported", record["model_grading_status"])

        fresh = run.model_grade(run_dir, invoke=grader)
        self.assertEqual("current", run.model_association_freshness(run_dir, fresh)["status"])
        self.assertEqual(first, json.loads(first_path.read_text()))
        self.assertEqual(1, len(list((run_dir / "model-grade-history").glob("*.json"))))

        (run_dir / "events.jsonl").write_text("assessed trace changed")
        self.assertEqual("stale", run.model_association_freshness(run_dir, fresh)["status"])
        (target / "src/main/java/gymmie/Existing.java").write_text("source changed after grading")
        self.assertEqual("stale", run.model_association_freshness(run_dir, fresh)["status"])
        with patch("sys.stdout", new_callable=io.StringIO) as output:
            run.command_inspect(SimpleNamespace(run=str(run_dir), output_dir=self.root / "runs"))
            final_inspect = json.loads(output.getvalue())
        self.assertEqual("stale", final_inspect["model_gradings"][0]["evidence_freshness"]["status"])

    def test_model_grader_is_limited_to_semantic_evidence_and_validates_output(self) -> None:
        target, run_dir, case = self._verify_fixture("grader")
        output = self.root / "runs"
        (run_dir / "diff.patch").write_text("diff evidence")
        (run_dir / "final-response.txt").write_text("candidate response")
        (run_dir / "events.jsonl").write_text(json.dumps({"type": "item.completed", "item": {"type": "agent_message", "text": "done"}}))
        run.verify_run(run_dir)
        report = json.loads((run_dir / "verification.json").read_text())
        report["commands"][0]["status"] = "fail"
        run.json_write(run_dir / "verification.json", report)
        selected, _refs = run.model_evidence(run_dir)
        self.assertNotIn("PREVIOUS JUDGMENT", run.build_grader_prompt(case, selected))
        observed = {}

        def invoke(prompt, schema, _run_dir):
            observed["prompt"], observed["schema"] = prompt, schema
            return {"judgments": [{"check_id": "feature_quality", "status": "pass",
                                   "evidence_refs": ["diff.patch"], "explanation": "Stub says pass.",
                                   "uncertainty": "Synthetic."}]}, {"status": "completed", "model_actual": "stub-model"}

        model_result = run.model_grade(run_dir, invoke=invoke)
        self.assertEqual("validated", model_result["status"])
        self.assertEqual("pass", model_result["judgments"][0]["status"])
        self.assertEqual("fail", json.loads((run_dir / "verification.json").read_text())["commands"][0]["status"])
        self.assertEqual("current", run.model_association_freshness(run_dir, model_result)["status"])
        self.assertIn("Semantic checks", observed["prompt"])
        self.assertNotIn("PREVIOUS JUDGMENT", observed["prompt"])
        self.assertEqual(["feature_quality"], observed["schema"]["properties"]["judgments"]["items"]["properties"]["check_id"]["enum"])

        def malformed(_prompt, _schema, _run_dir):
            return {"judgments": []}, {"status": "completed"}

        bad = run.model_grade(run_dir, invoke=malformed)
        self.assertEqual("malformed", bad["status"])
        self.assertEqual([], bad["judgments"])
        self.assertTrue((run_dir / "model-grade.json").is_file())
        self.assertEqual(1, len(list((run_dir / "model-grade-history").glob("*.json"))))

    def test_grader_output_validation_rejects_malformed_fields_without_raising(self) -> None:
        valid = {"judgments": [{"check_id": "feature_quality", "status": "pass",
                                "evidence_refs": ["diff.patch"], "explanation": "Evidence supports this.",
                                "uncertainty": "Some uncertainty."}]}
        self.assertEqual([], run.validate_grader_output(valid, ["feature_quality"], {"diff.patch"}))
        malformed_items = [
            {**valid["judgments"][0], "check_id": []},
            {**valid["judgments"][0], "status": []},
            {**valid["judgments"][0], "evidence_refs": [{}]},
            {**valid["judgments"][0], "explanation": []},
            {**valid["judgments"][0], "extra": "unsupported"},
        ]
        for item in malformed_items:
            with self.subTest(item=item):
                self.assertTrue(run.validate_grader_output({"judgments": [item]},
                                                           ["feature_quality"], {"diff.patch"}))
        self.assertTrue(run.validate_grader_output({"judgments": []}, ["feature_quality"], {"diff.patch"}))
        duplicate = {"judgments": [valid["judgments"][0], valid["judgments"][0]]}
        self.assertTrue(any("duplicate" in error for error in
                            run.validate_grader_output(duplicate, ["feature_quality"], {"diff.patch"})))
        unknown_ref = {"judgments": [{**valid["judgments"][0], "evidence_refs": ["not-present"]}]}
        self.assertTrue(any("unknown evidence reference" in error for error in
                            run.validate_grader_output(unknown_ref, ["feature_quality"], {"diff.patch"})))

    def test_archived_inputs_are_part_of_stale_evidence_detection(self) -> None:
        _target, run_dir, _case = self._verify_fixture("changed-input")
        meta = json.loads((run_dir / "metadata.json").read_text())
        meta["input_fingerprints"] = {"case.yaml": run.sha256((run_dir / "case.yaml").read_bytes())}
        run.json_write(run_dir / "metadata.json", meta)
        (run_dir / "case.yaml").write_text((run_dir / "case.yaml").read_text() + "\n# edited\n")
        self.assertTrue(run.run_stale(run_dir))

    def test_trace_claims_are_not_inferred_from_harness_activity(self) -> None:
        traces = [item for item in run.initial_checks(run.load_yaml(ISSUE_35)) if item["kind"] == "trace"]
        self.assertTrue(traces)
        unsupported = [item for item in traces if item["support"] == "not_observable"]
        self.assertTrue(all(item["status"] == "not_observable" for item in unsupported))
        self.assertEqual("not_observable", next(item for item in run.trace_assessments(
            run.load_yaml(ISSUE_35), []) if item["id"] == "IW-06")["status"])
        good_event = {"type": "item.completed", "item": {
            "type": "command_execution", "command": "./gradlew test", "exit_code": 0, "status": "completed"}}
        good = run.trace_assessments(run.load_yaml(ISSUE_35), [good_event])
        self.assertEqual("pass", next(item for item in good if item["id"] == "verification_invocation")["status"])
        self.assertEqual("not_observable", next(item for item in good if item["id"] == "IW-06")["status"])
        verbose_pytest = run.trace_assessments(run.load_yaml(ISSUE_35), [{"type": "item.completed", "item": {
            "type": "command_execution", "command": "pytest -v", "exit_code": 0, "status": "completed"}}])
        self.assertEqual("pass", next(item for item in verbose_pytest
                                       if item["id"] == "verification_invocation")["status"])
        for command in ("cat build.gradle", "./gradlew --version", "./gradlew test --version",
                        "./gradlew test -m", "./gradlew test --dry-run",
                        "./gradlew test --exclude-task=test",
                        "./gradlew test --exclude-task test", "./gradlew test -x test",
                        "./gradlew test -xtest", "echo pytest", "./gradlew test && echo done"):
            with self.subTest(command=command):
                events = [{"type": "item.completed", "item": {"type": "command_execution",
                          "command": command, "exit_code": 0, "status": "completed"}}]
                signal = next(item for item in run.trace_assessments(run.load_yaml(ISSUE_35), events)
                              if item["id"] == "verification_invocation")
                self.assertEqual("not_observable", signal["status"])
        failed = run.trace_assessments(run.load_yaml(ISSUE_35), [{"type": "item.completed", "item": {
            "type": "command_execution", "command": "./gradlew test", "exit_code": 1, "status": "failed"}}])
        self.assertEqual("fail", next(item for item in failed if item["id"] == "verification_invocation")["status"])

    def test_generated_run_directory_is_git_ignored(self) -> None:
        result = subprocess.run(["git", "check-ignore", "-q", "evals/implement-issues/runs/test-run"],
                                cwd=REPO, check=False)
        self.assertEqual(0, result.returncode)
        for path in ("evals/__pycache__/run.cpython-312.pyc",
                     "evals/__pycache__/test_run.cpython-312.pyc"):
            with self.subTest(path=path):
                ignored = subprocess.run(["git", "check-ignore", "-q", path], cwd=REPO, check=False)
                self.assertEqual(0, ignored.returncode)

    def test_curated_export_keeps_human_review_pending(self) -> None:
        _target, run_dir, _case = self._verify_fixture("curate")
        run.json_write(run_dir / "verification.json", {"status": "recorded", "human_review": "pending"})
        records = self.root / "records"
        run.command_record(SimpleNamespace(run=str(run_dir), output_dir=self.root / "runs", records_dir=records))
        curated = next(records.glob("*.json"))
        result = json.loads(curated.read_text())
        self.assertEqual("pending", result["human_review"])

    def test_record_export_inside_workspace_preserves_freshness_and_readiness(self) -> None:
        target = workspace(self.root / "record-workspace")
        case = run.load_yaml(REPO / "evals/implement-issues/cases/membership-switch-scope-decision.yaml")
        run_dir = self.root / "record-run"
        run_dir.mkdir()
        write_case(run_dir, case)
        output = self.root / "runs"
        run_id = "20260926T010203Z-a1b2c3d4"
        metadata = metadata_for(run_dir, target, output, case)
        metadata["run_id"] = run_id
        run.json_write(run_dir / "metadata.json", metadata)
        verification = {"run_id": run_id, "verification_id": "fixture-verification",
                        "status": "recorded", "commands": [], "checks": [], "human_review": "pending"}
        run.json_write(run_dir / "verification.json", verification)
        before = run.workspace_fingerprint(target, output)
        self.assertFalse(run.run_stale(run_dir))

        records_dir = target / "evals/implement-issues/records"
        run.command_record(SimpleNamespace(run=str(run_dir), output_dir=output, records_dir=records_dir))
        record = records_dir / f"{case['id']}-{run_id}.json"
        self.assertTrue(record.is_file())
        self.assertTrue(run.curated_record_artifact(record.relative_to(target).as_posix(), target))
        self.assertEqual(before, run.workspace_fingerprint(target, output))
        self.assertFalse(run.run_stale(run_dir))
        self.assertEqual([], run.dirty_relevant(target, output))

        with patch("sys.stdout", new_callable=io.StringIO) as inspected:
            run.command_inspect(SimpleNamespace(run=str(run_dir), output_dir=output))
            inspect_result = json.loads(inspected.getvalue())
        self.assertEqual("current", inspect_result["run_freshness"]["status"])
        self.assertEqual("current", inspect_result["verification_freshness"]["status"])

        ready_args = SimpleNamespace(cases_dir=REPO / "evals/implement-issues/cases",
                                     case=case["id"], workspace=target, output_dir=output, human_check=[])
        with patch.object(run.shutil, "which", return_value="codex"), \
                patch.object(run, "codex_login", return_value=True), \
                patch("sys.stdout", new_callable=io.StringIO) as readiness:
            self.assertEqual(0, run.command_ready(ready_args))
            self.assertIn('"status": "ready"', readiness.getvalue())

        unrelated = records_dir / "notes.txt"
        unrelated.write_text("not a curated harness record")
        self.assertEqual(["evals/implement-issues/records/notes.txt"],
                         [item["path"] for item in run.dirty_relevant(target, output)])


if __name__ == "__main__":
    unittest.main()
