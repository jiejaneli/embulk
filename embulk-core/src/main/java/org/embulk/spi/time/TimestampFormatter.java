package org.embulk.spi.time;

import java.util.Locale;
import com.google.common.base.Optional;
import org.jruby.util.RubyDateFormat;
import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.spi.util.LineEncoder;

public class TimestampFormatter
{
    public interface Task
    {
        @Config("default_timezone")
        @ConfigDefault("\"UTC\"")
        public String getDefaultTimeZoneId();

        // Using Joda-Time is deprecated, but the getter returns org.joda.time.DateTimeZone for plugin compatibility.
        // It won't be removed very soon at least until Embulk v0.10.
        @Deprecated
        public default org.joda.time.DateTimeZone getDefaultTimeZone() {
            if (getDefaultTimeZoneId() != null) {
                return TimeZoneIds.parseJodaDateTimeZone(getDefaultTimeZoneId());
            }
            else {
                return null;
            }
        }

        @Config("default_timestamp_format")
        @ConfigDefault("\"%Y-%m-%d %H:%M:%S.%6N %z\"")
        public String getDefaultTimestampFormat();
    }

    public interface TimestampColumnOption
    {
        @Config("timezone")
        @ConfigDefault("null")
        public Optional<String> getTimeZoneId();

        // Using Joda-Time is deprecated, but the getter returns org.joda.time.DateTimeZone for plugin compatibility.
        // It won't be removed very soon at least until Embulk v0.10.
        @Deprecated
        public default Optional<org.joda.time.DateTimeZone> getTimeZone() {
            if (getTimeZoneId().isPresent()) {
                return Optional.of(TimeZoneIds.parseJodaDateTimeZone(getTimeZoneId().get()));
            }
            else {
                return Optional.absent();
            }
        }

        @Config("format")
        @ConfigDefault("null")
        public Optional<String> getFormat();
    }

    private final RubyDateFormat dateFormat;
    private final org.joda.time.DateTimeZone timeZone;

    public TimestampFormatter(Task task, Optional<? extends TimestampColumnOption> columnOption)
    {
        this(
                columnOption.isPresent() ?
                    columnOption.get().getFormat().or(task.getDefaultTimestampFormat())
                    : task.getDefaultTimestampFormat(),
                columnOption.isPresent() ?
                    columnOption.get().getTimeZone().or(task.getDefaultTimeZone())
                    : task.getDefaultTimeZone());
    }

    public TimestampFormatter(final String format, final org.joda.time.DateTimeZone timeZone)
    {
        this.timeZone = timeZone;
        this.dateFormat = new RubyDateFormat(format, Locale.ENGLISH, true);
    }

    // Using Joda-Time is deprecated, but the getter returns org.joda.time.DateTimeZone for plugin compatibility.
    // It won't be removed very soon at least until Embulk v0.10.
    @Deprecated
    public org.joda.time.DateTimeZone getTimeZone()
    {
        return timeZone;
    }

    public void format(Timestamp value, LineEncoder encoder)
    {
        // TODO optimize by directly appending to internal buffer
        encoder.addText(format(value));
    }

    public String format(Timestamp value)
    {
        // TODO optimize by using reused StringBuilder
        dateFormat.setDateTime(new org.joda.time.DateTime(value.getEpochSecond()*1000, timeZone));
        dateFormat.setNSec(value.getNano());
        return dateFormat.format(null);
    }
}
