package pregrafer.rir.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Result of checking saved configuration; warnings do not prevent loading. */
public final class ConfigurationReport {
    private final int regionCount, activeRegionCount, indexEntryCount;
    private final List<String> warnings;

    ConfigurationReport(
            int regionCount, int activeRegionCount, int indexEntryCount, List<String> warnings) {
        this.regionCount = regionCount;
        this.activeRegionCount = activeRegionCount;
        this.indexEntryCount = indexEntryCount;
        this.warnings = Collections.unmodifiableList(new ArrayList<>(warnings));
    }

    public int getRegionCount() {
        return regionCount;
    }

    public int getActiveRegionCount() {
        return activeRegionCount;
    }

    public int getIndexEntryCount() {
        return indexEntryCount;
    }

    public List<String> getWarnings() {
        return warnings;
    }
}
