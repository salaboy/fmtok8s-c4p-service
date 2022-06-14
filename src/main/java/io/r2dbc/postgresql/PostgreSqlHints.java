package io.r2dbc.postgresql;

import org.springframework.nativex.hint.NativeHint;
import org.springframework.nativex.hint.TypeHint;
import org.springframework.nativex.type.NativeConfiguration;

import java.net.URI;
import java.time.Instant;
import java.time.ZonedDateTime;

@NativeHint(
        trigger = PostgresqlConnectionFactoryProvider.class,
        types = {
                @TypeHint(types = { Instant[].class, ZonedDateTime[].class, URI[].class }, access = {}),
        }
)
public class PostgreSqlHints implements NativeConfiguration {
}