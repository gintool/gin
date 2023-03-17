package gin.misc;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

/**
 * Just like a regular array list but indices 1...n rather than 0...n-1
 */
public class ArrayListOneBasedIndexing<T> extends ArrayList<T> {

    @Serial
    private static final long serialVersionUID = 3184581859826091364L;

    public ArrayListOneBasedIndexing(List<T> l) {
        super(l);
    }

    @Override
    public int indexOf(Object o) {
        int i = super.indexOf(o);
        if (i < 0) {
            return i;
        } else {
            return i + 1;
        }
    }

    @Override
    public int lastIndexOf(Object o) {
        int i = super.lastIndexOf(o);
        if (i < 0) {
            return i;
        } else {
            return i + 1;
        }
    }

    @Override
    public T get(int index) {
        return super.get(index - 1);
    }

    @Override
    public T set(int index, T element) {
        return super.set(index - 1, element);
    }

    @Override
    public void add(int index, T element) {
        super.add(index - 1, element);
    }

    @Override
    public T remove(int index) {
        return super.remove(index - 1);
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        return super.addAll(index - 1, c);
    }

    @Override
    protected void removeRange(int fromIndex, int toIndex) {
        super.removeRange(fromIndex - 1, toIndex - 1);
    }

    @Override
    public ListIterator<T> listIterator(int index) {
        return super.listIterator(index - 1);
    }

    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        return super.subList(fromIndex - 1, toIndex - 1);
    }


}
