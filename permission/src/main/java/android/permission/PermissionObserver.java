package android.permission;

import java.util.Observable;

public final class PermissionObserver extends Observable {

	private static PermissionObserver INSTANCE = new PermissionObserver();
	private PermissionObserver() {
	}

	public static PermissionObserver getInstance() {
		return INSTANCE;
	}
	public static PermissionObserver c() {
		return INSTANCE;
	}
	@Override
	public void notifyObservers(Object arg) {
		setChanged();
		super.notifyObservers(arg);
	}
}
