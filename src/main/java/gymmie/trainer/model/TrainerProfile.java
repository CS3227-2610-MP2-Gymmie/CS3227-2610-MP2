package gymmie.trainer.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import gymmie.model.Constraints;
import gymmie.model.exception.ValidationException;

/**
 * Trainer-specific details, separate from the shared account identity.
 *
 * @param accountId owner account identifier.
 * @param synopsis optional biography, empty when unset.
 * @param specializations ordered tags, with surrounding whitespace and case-insensitive duplicates removed.
 */
public record TrainerProfile(long accountId, String synopsis, List<String> specializations) {
    /** Validates details and takes an immutable copy of the normalized tags. */
    public TrainerProfile {
        Constraints.positiveId(accountId, "Trainer account ID");
        Constraints.required(synopsis, "Synopsis");
        Constraints.required(specializations, "Specializations");
        Map<String, String> tags = new LinkedHashMap<>();
        for (String tag : specializations) {
            if (tag == null || tag.isBlank()) {
                throw new ValidationException("Specialization tags must not be blank");
            }
            String value = tag.strip();
            tags.putIfAbsent(value.toLowerCase(Locale.ROOT), value);
        }
        specializations = List.copyOf(tags.values());
    }
}
