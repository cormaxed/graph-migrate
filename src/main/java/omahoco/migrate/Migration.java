package omahoco.migrate;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public final class Migration {
    private String fileName;
    private int version;
    private String checkSum;
    private List<String> statements;
    private Instant appliedAt;

    public Migration(final String fileName) {
        this.fileName = fileName;
    }

    public Migration(final String fileName, final String checkSum) {
        this.fileName = fileName;
        this.checkSum = checkSum;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(final int version) {
        this.version = version;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(final String fileName) {
        this.fileName = fileName;
    }

    public String getCheckSum() {
        return checkSum;
    }

    public void setCheckSum(final String checkSum) {
        this.checkSum = checkSum;
    }

    public void setStatements(final List<String> statements) {
        this.statements = statements;
    }

    public List<String> getStatements() {
        return statements;
    }

    public Instant getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(final Instant appliedAt) {
        this.appliedAt = appliedAt;
    }

    public String getContents() {
        if (statements == null) {
            return null;
        }

        return statements.stream().collect(Collectors.joining(System.lineSeparator()));
    }
}
