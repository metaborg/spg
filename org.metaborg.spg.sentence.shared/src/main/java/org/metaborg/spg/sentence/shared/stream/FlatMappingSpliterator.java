package org.metaborg.spg.sentence.shared.stream;

import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators.AbstractSpliterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Lazy flatMap implementation.
 *
 * Java's Stream.flatMap is not lazy. This method's flatMap takes a stream and a mapper and returns a new stream that
 * is evaluated lazily. Based on: https://stackoverflow.com/a/32767282/368220
 *
 * @param <E>
 * @param <S>
 */
public class FlatMappingSpliterator<E, S> extends AbstractSpliterator<E> implements Consumer<S> {
    private final Spliterator<S> source;
    private final Function<? super S, ? extends Stream<? extends E>> mapper;
    private Stream<? extends E> currentStream;
    private Spliterator<E> current;

    private FlatMappingSpliterator(Spliterator<S> source, Function<? super S, ? extends Stream<? extends E>> mapper) {
        super(source.estimateSize() + 100, source.characteristics() & ORDERED);

        this.source = source;
        this.mapper = mapper;
    }

    public static <T, R> Stream<R> flatMap(Stream<? extends T> stream, Function<? super T, ? extends Stream<? extends R>> mapper) {
        Objects.requireNonNull(stream);
        Objects.requireNonNull(mapper);

        return StreamSupport
                .stream(new FlatMappingSpliterator<>(stream.spliterator(), mapper), stream.isParallel())
                /*.onClose(stream::close)*/;
    }

    @SuppressWarnings("unchecked")
    private static <X> Spliterator<X> sp(Stream<? extends X> stream) {
        return stream != null ? ((Stream<X>) stream).spliterator() : null;
    }

    private void closeCurrent() {
        try {
            currentStream.close();
        } finally {
            currentStream = null;
            current = null;
        }
    }

    @Override
    public void accept(S s) {
        current = sp(currentStream = mapper.apply(s));
    }

    @Override
    public boolean tryAdvance(Consumer<? super E> action) {
        do {
            if (current != null) {
                if (current.tryAdvance(action)) {
                    return true;
                }
                closeCurrent();
            }
        } while (source.tryAdvance(this));

        return false;
    }

    @Override
    public void forEachRemaining(Consumer<? super E> action) {
        if (current != null) {
            current.forEachRemaining(action);
            closeCurrent();
        }

        source.forEachRemaining(s -> {
            try (Stream<? extends E> stream = mapper.apply(s)) {
                if (stream != null) {
                    stream.spliterator().forEachRemaining(action);
                }
            }
        });
    }

    @Override
    public Spliterator<E> trySplit() {
        Spliterator<S> split = source.trySplit();

        if (split == null) {
            Spliterator<E> prefix = current;
            while (prefix == null && source.tryAdvance(s -> current = sp(mapper.apply(s)))) {
                prefix = current;
            }
            current = null;
            return prefix;
        }

        FlatMappingSpliterator<E, S> prefix = new FlatMappingSpliterator<>(split, mapper);

        if (current != null) {
            prefix.current = current;
            current = null;
        }

        return prefix;
    }
}
