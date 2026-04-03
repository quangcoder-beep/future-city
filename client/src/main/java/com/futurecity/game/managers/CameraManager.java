package com.futurecity.game.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.futurecity.game.entities.GameObject;
import com.futurecity.shared.config.GameConstants;

public class CameraManager implements InputProcessor {
    public PerspectiveCamera camera;
    private CameraViewManager currentView = CameraViewManager.THIRD_PERSON;

    // Smoothness control variables
    private final float rotationLerp = 0.3f; // Mouse rotation response
    private final float positionLerp = 0.5f; // Camera lag when following the character

    // Camera rotation angles
    private float yaw = 0;
    private float pitch = -10f; // Default viewing angle slightly tilted down
    private float targetYaw = 0;
    private float targetPitch = -10f;
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

    // Camera distance (Zoom) - Get default value from GameConstants
    private float currentDistance = GameConstants.CAM_DISTANCE_TPS;

    // Temporary variables for vector calculation (avoid continuous memory
    // allocation)
    private final Vector3 tmpPosition = new Vector3();
    private final Vector3 tmpDirection = new Vector3();
    private final Vector3 tmpRight = new Vector3();

    public CameraManager(PerspectiveCamera camera) {
        this.camera = camera;

        // Camera frustum configuration for the vast open world map
        this.camera.near = 1f; // Increase near to avoid clipping with objects at close range
        this.camera.far = 5000f; // Increase far to see the entire large map

        // Lock the mouse to the center of the screen to rotate the camera
        Gdx.input.setCursorCatched(true);
        Gdx.input.setInputProcessor(this);
    }

    public void update(float deltaTime, GameObject target) {
        // 1. Handle input
        handleInput();

        // 2. Smooth rotation
        yaw = MathUtils.lerp(yaw, targetYaw, rotationLerp);
        pitch = MathUtils.lerp(pitch, targetPitch, rotationLerp);

        // 3. Update camera position based on view mode
        if (currentView == CameraViewManager.FIRST_PERSON) {
            updateFirstPerson(target);
        } else {
            updateThirdPerson(target);
        }

        // Always call update() last to apply changes to the matrix
        camera.update();
    }

    private void handleInput() {
        if (isUiFocused()) return;
        // Key C: Switch view mode
        if (Gdx.input.isKeyJustPressed(Input.Keys.C)) {
            switchView();
        }

        // Key B: Toggle mouse lock (for debugging or exiting)
        if (Gdx.input.isKeyJustPressed(Input.Keys.B)) {
            boolean isCaught = Gdx.input.isCursorCatched();
            Gdx.input.setCursorCatched(!isCaught);
        }

        // Rotate camera with mouse (only when mouse is locked)
        if (Gdx.input.isCursorCatched()) {
            float deltaX = Gdx.input.getDeltaX() * 0.15f;
            float deltaY = Gdx.input.getDeltaY() * 0.15f;

            targetYaw -= deltaX;
            targetPitch -= deltaY;

            // Limit the viewing angle up/down (Stronger limit for looking up to avoid underground clipping)
            targetPitch = MathUtils.clamp(targetPitch, -89f, 45f);
        }
    }

    private void updateFirstPerson(GameObject target) {
        // --- FIRST PERSON VIEW ---

        // Position: Character position + eye height (Get from GameConstants)
        tmpPosition.set(target.position).add(0, GameConstants.CAM_EYE_HEIGHT, 0);
        camera.position.set(tmpPosition);

        camera.up.set(Vector3.Y);

        // Viewing direction: Calculate vector from Yaw and Pitch angles
        float dirX = MathUtils.cosDeg(pitch) * MathUtils.sinDeg(yaw);
        float dirY = MathUtils.sinDeg(pitch);
        float dirZ = MathUtils.cosDeg(pitch) * MathUtils.cosDeg(yaw);

        camera.direction.set(dirX, dirY, dirZ).nor();
    }

    private void updateThirdPerson(GameObject target) {
        // --- THIRD PERSON VIEW ---

        // 1. Pivot: Rotation point (Character's shoulder/neck)
        Vector3 pivot = tmpPosition.set(target.position).add(0, GameConstants.CAM_PIVOT_HEIGHT, 0);

        // 2. Calculate viewing direction (Forward Vector)
        float dirX = MathUtils.cosDeg(pitch) * MathUtils.sinDeg(yaw);
        float dirY = MathUtils.sinDeg(pitch);
        float dirZ = MathUtils.cosDeg(pitch) * MathUtils.cosDeg(yaw);
        tmpDirection.set(dirX, dirY, dirZ).nor();

        // 3. Calculate camera position: From pivot back by a distance
        Vector3 camPos = new Vector3(pivot).sub(new Vector3(tmpDirection).scl(currentDistance));

        // 4. Calculate camera offset (Camera offset to the right)
        // Vector Right = Cross Product of Forward and Up (Y)
        tmpRight.set(tmpDirection).crs(Vector3.Y).nor().scl(-1);

        // Add shoulder offset
        camPos.add(
                tmpRight.x * GameConstants.CAM_SHOULDER_OFFSET,
                tmpRight.y * GameConstants.CAM_SHOULDER_OFFSET,
                tmpRight.z * GameConstants.CAM_SHOULDER_OFFSET);

        // 5. Ground Collision Check (Simple Y-Plane Clamp)
        if (camPos.y < 2.0f) {
            camPos.y = 2.0f;
        }

        // 6. Update camera (use Lerp for smooth movement)
        camera.position.lerp(camPos, positionLerp);
        camera.direction.set(tmpDirection);
        camera.up.set(Vector3.Y);
    }

    public void switchView() {
        if (currentView == CameraViewManager.FIRST_PERSON) {
            currentView = CameraViewManager.THIRD_PERSON;
            currentDistance = GameConstants.CAM_DISTANCE_TPS;
        } else {
            currentView = CameraViewManager.FIRST_PERSON;
            currentDistance = 0.1f; // Very close for first-person
        }
    }

    public CameraViewManager getCurrentView() {
        return currentView;
    }

    // Getter for yaw (needed for minimap rotation)
    public float getYaw() {
        return yaw;
    }

    // --- CĂC HĂ€M Cá»¦A INPUT PROCESSOR (GIá»® NGUYĂN) ---
    @Override
    public boolean scrolled(float amountX, float amountY) {
        return false;
    }

    @Override
    public boolean keyDown(int keycode) {
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }
}
