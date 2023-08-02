package myutils;

public class MyAtomicEnum<E extends Enum<E>> {
    private volatile E value;

    public MyAtomicEnum(Class<E> enumClass, E initialValue) {
        validateEnum(enumClass);
        value = initialValue;
    }

    public E get() {
        return value;
    }

    public void set(E newValue) {
        value = newValue;
    }

    public boolean compareAndSet(E expectedValue, E newValue) {
        synchronized (this) {
            if (value == expectedValue) {
                value = newValue;
                return true;
            }
            return false;
        }
    }

    private void validateEnum(Class<E> enumClass) {
        if (!enumClass.isEnum()) {
            throw new IllegalArgumentException("Class must be an enum");
        }
    }
}
