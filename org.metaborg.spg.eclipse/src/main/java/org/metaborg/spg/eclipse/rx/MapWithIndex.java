package org.metaborg.spg.eclipse.rx;

import java.util.Iterator;

import org.metaborg.spg.eclipse.rx.MapWithIndex.Indexed;

import rx.Observable;
import rx.Observable.Transformer;
import rx.functions.Func2;

public final class MapWithIndex<T> implements Transformer<T, Indexed<T>> {
    private static class Holder {
        static final MapWithIndex<?> INSTANCE = new MapWithIndex<Object>();
    }

    @SuppressWarnings("unchecked")
    public static <T> MapWithIndex<T> instance() {
        return (MapWithIndex<T>) Holder.INSTANCE;
    }

	@Override
	public Observable<? extends Indexed<T>> call(Observable<? extends T> source) {
        return source.zipWith(NaturalNumbers.instance(), new Func2<T, Long, Indexed<T>>() {
            @Override
            public Indexed<T> call(T t, Long n) {
                return new Indexed<T>(t, n);
            }
        });
	}

    public static final class Indexed<T> {
        private final long index;
        private final T value;

        public Indexed(T value, long index) {
            this.index = index;
            this.value = value;
        }

        @Override
        public String toString() {
            return index + "->" + value;
        }

        public long index() {
            return index;
        }

        public T value() {
            return value;
        }
    }

    private static final class NaturalNumbers implements Iterable<Long> {
        private static class Holder {
            static final NaturalNumbers INSTANCE = new NaturalNumbers();
        }

        static NaturalNumbers instance() {
            return Holder.INSTANCE;
        }

        @Override
        public Iterator<Long> iterator() {
            return new Iterator<Long>() {

                private long n = 0;

                @Override
                public boolean hasNext() {
                    return true;
                }

                @Override
                public Long next() {
                    return n++;
                }

                @Override
                public void remove() {
                    throw new RuntimeException("not supported");
                }
            };
        }
    }
}
