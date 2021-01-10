package org.schabi.newpipelegacy.about;

/**
 * Class containing information about standard software licenses.
 */
public final class StandardLicenses {
    public static final License GPL3
            = new License("GNU General Public License, Version 3.0", "GPLv3", "gpl_3.html");
    public static final License APACHE2
            = new License("Apache License, Version 2.0", "ALv2", "apache2.html");
    public static final License MPL2
            = new License("Mozilla Public License, Version 2.0", "MPL 2.0", "mpl2.html");
    public static final License MIT
            = new License("MIT License", "MIT", "mit.html");
    public static final License EPL1
            = new License("Eclipse Public License, Version 1.0", "EPL 1.0", "epl1.html");

    private StandardLicenses() { }
}
