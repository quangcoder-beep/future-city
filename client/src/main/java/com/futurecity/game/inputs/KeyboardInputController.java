package com.futurecity.game.inputs;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;

/**
 * KeyboardInputController
 * <p>
 * Implementation of IInputController for keyboard (PC/Laptop).
 * This class is responsible for mapping keys to actions.
 */
public class KeyboardInputController extends com.badlogic.gdx.InputAdapter implements IInputController {
    private boolean up, down, left, right;
    private boolean isRun, isJump;
    private java.util.function.Supplier<Boolean> uiFocusChecker;

    /**
     * Set a checker to determine if the UI currently has focus on an input field.
     */
    public void setUiFocusChecker(java.util.function.Supplier<Boolean> checker) {
        this.uiFocusChecker = checker;
    }

    private boolean isUiFocused() {
        return uiFocusChecker != null && uiFocusChecker.get();
    }

    @Override
    public boolean keyDown(int keycode) {
        if (isUiFocused()) return false;

        boolean handled = true;
        switch (keycode) {
            case Input.Keys.W:
                up = true;
                break;
            case Input.Keys.S:
                down = true;
                break;
            case Input.Keys.A:
                left = true;
                break;
            case Input.Keys.D:
                right = true;
                break;
            case Input.Keys.E:
                isRun = true;
                break;
            case Input.Keys.SPACE:
                isJump = true;
                break;
            default:
                handled = false;
        }
        if (handled) {
            System.out.println("DEBUG-INPUT: KeyDown=" + Input.Keys.toString(keycode) + " (up=" + up + ", down=" + down
                    + ", left=" + left + ", right=" + right + ")");
        }
        return handled;
    }

    @Override
    public boolean keyUp(int keycode) {
        boolean handled = true;
        switch (keycode) {
            case Input.Keys.W:
                up = false;
                break;
            case Input.Keys.S:
                down = false;
                break;
            case Input.Keys.A:
                left = false;
                break;
            case Input.Keys.D:
                right = false;
                break;
            case Input.Keys.E:
                isRun = false;
                break;
            case Input.Keys.SPACE:
                isJump = false;
                break;
            default:
                handled = false;
        }
        if (handled) {
            System.out.println("DEBUG-INPUT: KeyUp=" + Input.Keys.toString(keycode) + " (up=" + up + ", down=" + down
                    + ", left=" + left + ", right=" + right + ")");
        }
        return handled;
    }

    /**
     * Re-sync keyboard state. Useful when closing UI to prevent stuck keys.
     */
    public void clearInput() {
        up = false;
        down = false;
        left = false;
        right = false;
        isRun = false;
        isJump = false;
        System.out.println("DEBUG: KeyboardInputController state cleared.");
    }

    @Override
    public float getHorizontal() {
        return (right ? 1 : 0) - (left ? 1 : 0);
    }

    @Override
    public float getVertical() {
        return (up ? 1 : 0) - (down ? 1 : 0);
    }

    @Override
    public boolean isRunPressed() {
        return isRun;
    }

    @Override
    public boolean isJumpPressed() {
        return isJump;
    }

    @Override
    public boolean isInteractJustPressed() {
        return Gdx.input.isKeyJustPressed(Input.Keys.F);
    }
}
