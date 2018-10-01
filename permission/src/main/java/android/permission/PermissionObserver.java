package android.permission;

import java.util.Observable;

final class PermissionObserver extends Observable {

    private static PermissionObserver INSTANCE = new PermissionObserver();

    private PermissionObserver() { }

    static PermissionObserver getInstance() { return INSTANCE; }

    @Override
    public void notifyObservers(Object arg) {
        setChanged();
        super.notifyObservers(arg);
    }
}
