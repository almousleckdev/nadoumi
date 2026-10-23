package com.nadoumi.common.scholarship;

import java.util.Set;

/**
 * The scholarship categories that exist (CSC, provincial, university, ...). The scholarship module owns them; other
 * modules, such as programmes that say which types of scholarship fund them, depend on this interface and never on
 * the scholarship tables.
 */
public interface ScholarshipCategoryCatalogue {

    /** The codes of every known category. */
    Set<String> codes();
}
