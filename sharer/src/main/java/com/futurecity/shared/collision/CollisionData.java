package com.futurecity.shared.collision;

import com.badlogic.gdx.math.Vector3;
import com.futurecity.shared.config.GameConstants;
import com.badlogic.gdx.math.collision.BoundingBox;
import java.util.ArrayList;
import java.util.List;

/**
 * "Kho chá»©a cĂ¡c há»™p va cháº¡m" (Bounding Boxes) cá»§a táº¥t cáº£ cĂ¡c váº­t thá»ƒ trĂªn báº£n
 * Class nĂ y Ä‘Æ°á»£c dĂ¹ng bá»Ÿi:
 * - Server: Äá»ƒ xá»­ lĂ½ collision authoritative
 * - Client: (Optional) CĂ³ thá»ƒ dĂ¹ng Ä‘á»ƒ validate hoáº·c fallback
 * 
 * LĂ½ do khĂ´ng load tá»« file 3D:
 * - Server headless khĂ´ng cĂ³ OpenGL context
 * - Server chá»‰ cáº§n collision data, khĂ´ng cáº§n mesh/texture
 * - Load nhanh hÆ¡n nhiá»u
 */
public class CollisionData {

    /**
     * Tráº£ vá» danh sĂ¡ch táº¥t cáº£ obstacles trong map testcity.
     * 
     * TODO: Hiá»‡n táº¡i Ä‘ang dĂ¹ng data máº«u. Cáº§n Ä‘iá»n collision boxes thá»±c táº¿
     * báº±ng cĂ¡ch:
     * 1. Cháº¡y game, quan sĂ¡t vá»‹ trĂ­ cĂ¡c nhĂ /tĂ²a nhĂ 
     * 2. Hoáº·c táº¡o tool nhá» Ä‘á»ƒ extract tá»« GLB file
     * 3. Copy positions vĂ o Ä‘Ă¢y
     */
    public static List<BoundingBox> getMapObstacles() {
        List<BoundingBox> obstacles = new ArrayList<>();

        // ===== COLLISION DATA FROM CLIENT MAP =====
        // Extracted from MapManager - actual obstacle positions from testcity.glb
        obstacles.add(box(-1.0f, 0.25f, -1.0f, 2.0f, 8.0f, 2.0f));
        obstacles.add(box(1.75f, 0.25f, -1.75f, 1.5f, 6.0f, 1.5f));
        obstacles.add(box(-2.9f, 0.25f, 0.9f, 1.8000001f, 7.0f, 1.1999999f));
        obstacles.add(box(7.931267f, 0.25f, -0.5000001f, 1.0f, 2.05f, 1.0000002f));
        obstacles.add(box(7.00484f, 0.25f, -5.1215963f, 1.366025f, 2.05f, 1.3660252f));
        obstacles.add(box(3.7359812f, 0.25f, -8.336935f, 1.3660257f, 2.05f, 1.3660259f));
        obstacles.add(box(-0.50000006f, 0.25f, -9.491107f, 1.0000001f, 2.05f, 1.0f));
        obstacles.add(box(-5.072516f, 0.25f, -8.285854f, 1.3660259f, 2.05f, 1.3660254f));
        obstacles.add(box(-7.6895046f, 0.25f, -4.7282124f, 1.3660259f, 2.05f, 1.3660252f));
        obstacles.add(box(-9.4568f, 0.25f, -0.5000001f, 1.0f, 2.05f, 1.0000002f));
        obstacles.add(box(-8.412918f, 0.25f, 3.7798502f, 1.3660259f, 2.05f, 1.3660257f));
        obstacles.add(box(-4.7080183f, 0.25f, 6.288501f, 1.3660257f, 2.05f, 1.366025f));
        obstacles.add(box(-0.5f, 0.25f, 7.924115f, 1.0f, 2.05f, 1.0f));
        obstacles.add(box(3.460243f, 0.25f, 6.4933167f, 1.3660252f, 2.05f, 1.3660259f));
        obstacles.add(box(6.6925297f, 0.25f, 3.5752585f, 1.366025f, 2.05f, 1.3660257f));
        obstacles.add(box(9.6f, 0.25f, 9.762564f, 2.8000002f, 2.6499999f, 2.4748726f));
        obstacles.add(box(12.292893f, 0.25f, 12.292893f, 1.4142132f, 2.05f, 1.4142132f));
        obstacles.add(box(-6.764944f, 0.2303556f, -0.84323007f, 0.69550514f, 0.9413097f, 0.7143478f));
        obstacles.add(box(4.422123f, 0.30058163f, 6.92725f, 0.72179794f, 0.85022f, 0.99566174f));
        obstacles.add(box(-0.4752494f, 0.27283704f, -4.665722f, 0.7655921f, 0.8436127f, 0.7887552f));
        obstacles.add(box(-7.1768093f, 0.22421633f, -4.1695023f, 0.45485973f, 0.83295476f, 0.5065634f));
        obstacles.add(box(1.5839205f, 0.2080603f, -4.6160808f, 0.5665848f, 1.0727954f, 0.76696897f));
        obstacles.add(box(1.366038f, 0.19534484f, -6.25752f, 0.8803915f, 1.4691525f, 1.0280218f));
        obstacles.add(box(-4.6288567f, 0.21623798f, 0.60123813f, 0.6469555f, 0.8922555f, 0.59630346f));
        obstacles.add(box(-1.2719958f, 0.21652728f, -5.5691233f, 1.1023573f, 1.4243882f, 0.9631386f));
        obstacles.add(box(-2.7096748f, 0.26201215f, -4.7642703f, 0.6888156f, 0.8091222f, 0.7822876f));
        obstacles.add(box(7.2354856f, 0.24219635f, -1.3250046f, 0.9001832f, 1.0417445f, 0.89782506f));
        obstacles.add(box(5.744606f, 0.2363205f, -3.6709604f, 0.5818863f, 0.761413f, 0.4878173f));
        obstacles.add(box(-3.9417672f, 0.2436508f, 1.6918484f, 0.67436314f, 0.8933102f, 0.700436f));
        obstacles.add(box(0.39023829f, 0.27785033f, 6.94053f, 0.975343f, 0.9653283f, 0.931756f));
        obstacles.add(box(-3.5590224f, 0.29436368f, -6.977833f, 1.1385093f, 1.1063173f, 1.1988955f));
        obstacles.add(box(3.0615983f, 0.23722729f, 3.695448f, 0.71548414f, 0.8353194f, 0.715168f));
        obstacles.add(box(-7.6909924f, 0.22831726f, 1.7877125f, 0.7094717f, 1.0304257f, 0.72417986f));
        obstacles.add(box(10.15037f, 0.22274306f, 14.630605f, 0.7153816f, 1.1071796f, 0.81824875f));
        obstacles.add(box(11.392411f, 0.22314367f, 12.090324f, 0.6956444f, 0.97574663f, 0.7914982f));
        obstacles.add(box(12.552727f, 0.20677012f, 14.153279f, 0.71559525f, 1.1013186f, 0.6235275f));
        obstacles.add(box(11.344772f, 0.26815566f, 11.456647f, 0.8242111f, 1.1811785f, 1.0769768f));
        obstacles.add(box(11.026328f, 0.24373546f, 12.361985f, 0.7783661f, 1.0684739f, 0.8406544f));
        obstacles.add(box(8.863125f, 0.22008103f, 10.303156f, 0.6215439f, 0.9116183f, 0.66420174f));
        obstacles.add(box(12.403367f, 0.21076474f, 11.115437f, 0.5660496f, 0.9753045f, 0.5959797f));
        obstacles.add(box(11.810597f, 0.24913087f, 12.192056f, 0.770504f, 0.93880117f, 0.6687069f));
        obstacles.add(box(9.47592f, 0.2486631f, 9.453081f, 0.61471367f, 0.80599403f, 0.61927414f));
        obstacles.add(box(11.193258f, 0.29745042f, 9.809403f, 0.80794334f, 0.9818299f, 0.9530239f));
        obstacles.add(box(10.931541f, 0.26536268f, 12.414531f, 0.50696564f, 0.74051696f, 0.65779495f));
        obstacles.add(box(10.2324f, 0.23541623f, 9.353462f, 0.9968357f, 1.2224374f, 0.8231945f));
        obstacles.add(box(10.702179f, 0.2552358f, 7.7632833f, 0.5652981f, 0.7567153f, 0.64839315f));
        obstacles.add(box(9.856657f, 0.27999732f, 10.272495f, 0.6774311f, 0.68153274f, 0.5801258f));
        obstacles.add(box(15.190184f, 0.25475335f, 10.566684f, 0.722929f, 1.0168567f, 0.9873886f));
        obstacles.add(box(-11.434085f, 0.27114543f, -9.898505f, 1.0312805f, 1.1566123f, 1.1074581f));
        obstacles.add(box(-11.050434f, 0.2987793f, -11.073353f, 0.80270386f, 0.83918136f, 0.6621151f));
        obstacles.add(box(-10.649152f, 0.26875785f, -7.9957385f, 0.6996269f, 1.0630484f, 0.74395084f));
        obstacles.add(box(-10.338479f, 0.20962372f, -10.520108f, 0.7646313f, 1.2046908f, 1.000391f));
        obstacles.add(box(-10.569777f, 0.2156007f, -11.511404f, 0.6947231f, 1.0135787f, 0.84072685f));
        obstacles.add(box(-8.068765f, 0.24981047f, -8.997406f, 0.5983887f, 0.89234495f, 0.8080139f));
        obstacles.add(box(-8.606629f, 0.2292431f, -12.86374f, 0.60847807f, 0.82501763f, 0.53676414f));
        obstacles.add(box(-10.136349f, 0.21493274f, -10.785254f, 0.68276405f, 0.9026596f, 0.5736065f));
        obstacles.add(box(-8.808613f, 0.29099265f, -12.807808f, 1.0440674f, 1.112163f, 1.0055943f));
        obstacles.add(box(-11.770206f, 0.22191851f, -9.417539f, 0.57795334f, 0.98906034f, 0.72084236f));
        obstacles.add(box(-11.188277f, 0.19914827f, -8.8236685f, 1.0209122f, 1.4496102f, 1.056961f));
        obstacles.add(box(-10.301608f, 0.29448754f, -10.371783f, 0.5967598f, 0.79630715f, 0.7699299f));
        obstacles.add(box(11.8f, -0.20000052f, 9.799999f, 6.4000006f, 6.4000015f, 0.40000153f));

        System.out.println("CollisionData: Loaded " + obstacles.size() + " obstacle boxes");

        // FIX: Scale táº¥t cáº£ obstacle theo MAP_SCALE (80f) Ä‘á»ƒ khá»›p vá»›i world tháº­t
        // Dá»¯ liá»‡u raw á»Ÿ trĂªn lĂ  tá»« model GLTF chÆ°a scale
        float scale = GameConstants.MAP_SCALE;
        List<BoundingBox> scaledObstacles = new ArrayList<>();

        for (BoundingBox raw : obstacles) {
            BoundingBox scaled = new BoundingBox();
            Vector3 min = new Vector3();
            Vector3 max = new Vector3();

            raw.getMin(min);
            raw.getMax(max);

            min.scl(scale);
            max.scl(scale);

            scaled.set(min, max);
            scaledObstacles.add(scaled);
        }

        System.out.println("CollisionData: Scaled " + scaledObstacles.size() + " obstacles by " + scale);

        return scaledObstacles;
    }

    /**
     * Helper method Ä‘á»ƒ táº¡o BoundingBox dá»… dĂ ng hÆ¡n.
     * 
     * @param x      X position (bottom-left corner)
     * @param y      Y position (bottom)
     * @param z      Z position (bottom-left corner)
     * @param width  Width (X axis)
     * @param height Height (Y axis)
     * @param depth  Depth (Z axis)
     * @return BoundingBox má»›i
     */
    private static BoundingBox box(float x, float y, float z,
            float width, float height, float depth) {
        Vector3 min = new Vector3(x, y, z);
        Vector3 max = new Vector3(x + width, y + height, z + depth);
        return new BoundingBox(min, max);
    }
}
