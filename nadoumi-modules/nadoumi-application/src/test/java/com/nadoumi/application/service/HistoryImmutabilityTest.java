package com.nadoumi.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.nadoumi.application.mapper.ApplicationDecisionMapper;
import com.nadoumi.application.mapper.ApplicationEventMapper;
import com.nadoumi.application.mapper.ApplicationSnapshotMapper;
import com.nadoumi.application.mapper.ApplicationStageHistoryMapper;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * No mapper statement updates or deletes a {@code *_history} / {@code *_event} /
 * {@code *_decision} / {@code *_snapshot} row (append-only, CLAUDE.md §9). Enforced
 * structurally: these four mapper interfaces declare only {@code find*}/{@code insert}.
 */
class HistoryImmutabilityTest {

    private static final List<Class<?>> APPEND_ONLY_MAPPERS = List.of(
            ApplicationStageHistoryMapper.class, ApplicationEventMapper.class,
            ApplicationDecisionMapper.class, ApplicationSnapshotMapper.class);

    @Test
    void append_only_mappers_declare_no_update_or_delete_method() {
        for (Class<?> mapperType : APPEND_ONLY_MAPPERS) {
            List<String> mutating = Stream.of(mapperType.getDeclaredMethods())
                    .map(Method::getName)
                    .filter(name -> name.toLowerCase(java.util.Locale.ROOT).contains("update")
                            || name.toLowerCase(java.util.Locale.ROOT).contains("delete"))
                    .toList();
            assertThat(mutating).as("mutating method on append-only mapper " + mapperType.getSimpleName()).isEmpty();
        }
    }

    @Test
    void every_declared_method_on_an_append_only_mapper_is_find_or_insert() {
        for (Class<?> mapperType : APPEND_ONLY_MAPPERS) {
            for (Method m : mapperType.getDeclaredMethods()) {
                assertThat(m.getName()).as(mapperType.getSimpleName() + "." + m.getName())
                        .matches("^(find|insert).*");
            }
        }
    }
}
