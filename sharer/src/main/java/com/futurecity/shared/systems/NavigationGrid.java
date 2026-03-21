package com.futurecity.shared.systems;

import com.badlogic.gdx.math.Vector3;
import java.util.*;

/**
 * Ma tráº­n 2D Ä‘áº¡i diá»‡n cho báº£n Ä‘á»“ tĂ¬m Ä‘Æ°á»ng.
 * Sá»­ dá»¥ng byte[][] Ä‘á»ƒ tiáº¿t kiá»‡m bá»™ nhá»› (1000x1000 = 1MB).
 * Class nĂ y náº±m á»Ÿ sharer Ä‘á»ƒ cáº£ Client vĂ  Server Ä‘á»u dĂ¹ng
 * Ä‘Æ°á»£c.
 */
public class NavigationGrid {
    public static final byte EMPTY = 0;
    public static final byte ROAD = 1;
    public static final byte OBSTACLE = 2;

    private final byte[][] matrix;
    private final int width;
    private final int height;
    private final float gridSize;

    // Tá»a Ä‘á»™ world cá»§a gĂ³c (0,0) trong ma tráº­n
    private float worldMinX;
    private float worldMinZ;

    public NavigationGrid(int width, int height, float gridSize, float worldMinX, float worldMinZ) {
        this.width = width;
        this.height = height;
        this.gridSize = gridSize;
        this.worldMinX = worldMinX;
        this.worldMinZ = worldMinZ;
        this.matrix = new byte[width][height];
    }

    public void setCell(int x, int z, byte type) {
        if (isInBounds(x, z)) {
            matrix[x][z] = type;
        }
    }

    public byte getCell(int x, int z) {
        if (isInBounds(x, z))
            return matrix[x][z];
        return OBSTACLE;
    }

    public boolean isWalkable(int x, int z) {
        return getCell(x, z) == ROAD;
    }

    public boolean isInBounds(int x, int z) {
        return x >= 0 && x < width && z >= 0 && z < height;
    }

    /**
     * Chuyá»ƒn World (x, z) -> Grid (x, z)
     */
    public int worldToGridX(float worldX) {
        return (int) ((worldX - worldMinX) / gridSize);
    }

    public int worldToGridZ(float worldZ) {
        return (int) ((worldZ - worldMinZ) / gridSize);
    }

    /**
     * Chuyá»ƒn Grid (x, z) -> World (tĂ¢m Ă´)
     */
    public float gridToWorldX(int x) {
        return x * gridSize + worldMinX + gridSize / 2;
    }

    public float gridToWorldZ(int z) {
        return z * gridSize + worldMinZ + gridSize / 2;
    }

    /**
     * TĂ¬m Ă´ ROAD gáº§n nháº¥t báº±ng thuáº­t toĂ¡n BFS
     */
    public GridPos findNearestRoad(float worldX, float worldZ, int maxSearchRadius) {
        int startX = worldToGridX(worldX);
        int startZ = worldToGridZ(worldZ);


        if (isWalkable(startX, startZ)) {

            return new GridPos(startX, startZ);
        }

        Queue<GridPos> queue = new LinkedList<>();

        // Tá»I Æ¯U HIá»†U NÄ‚NG: Chá»‰ cáº¥p phĂ¡t máº£ng cá»¥c bá»™ thay vĂ¬ máº£ng
        // cá»§a toĂ n báº£n Ä‘á»“
        // Viá»‡c cáº¥p phĂ¡t máº£ng 2D (VĂ­ dá»¥: 2000x2000) má»—i láº§n click sinh ra
        // hĂ ng ngĂ n
        // object rĂ¡c gĂ¢y Ä‘á»©ng mĂ n hĂ¬nh.
        int windowSize = maxSearchRadius * 2 + 1;
        boolean[][] visited = new boolean[windowSize][windowSize];

        queue.add(new GridPos(startX, startZ));
        visited[maxSearchRadius][maxSearchRadius] = true;

        int[][] dirs = { { 0, 1 }, { 0, -1 }, { 1, 0 }, { -1, 0 }, { 1, 1 }, { 1, -1 }, { -1, 1 }, { -1, -1 } };

        while (!queue.isEmpty()) {
            GridPos curr = queue.poll();

            int dist = Math.max(Math.abs(curr.x - startX), Math.abs(curr.z - startZ));
            if (dist > maxSearchRadius)
                continue;

            for (int[] d : dirs) {
                int nx = curr.x + d[0];
                int nz = curr.z + d[1];

                if (isInBounds(nx, nz)) {
                    // Tá»a Ä‘á»™ tÆ°Æ¡ng Ä‘á»‘i trĂªn máº£ng Window
                    int localX = nx - startX + maxSearchRadius;
                    int localZ = nz - startZ + maxSearchRadius;

                    if (localX >= 0 && localX < windowSize && localZ >= 0 && localZ < windowSize) {
                        if (!visited[localX][localZ]) {
                            if (matrix[nx][nz] == ROAD) {
                                return new GridPos(nx, nz);
                            }
                            visited[localX][localZ] = true;
                            queue.add(new GridPos(nx, nz));
                        }
                    }
                }
            }
        }

        return null; // BFS failed, no road within radius
    }

    /**
     * A* lĂ  Dijkstra nhÆ°ng cĂ³ thĂªm â€œÄ‘á»‹nh hÆ°á»›ng vá» Ä‘Ă­châ€.
     * 
     * | Dijkstra: tĂ¬m Ä‘Æ°á»ng ngáº¯n nháº¥t nhÆ°ng lan ra má»i hÆ°á»›ng.
     * | A*: váº«n tĂ¬m Ä‘Æ°á»ng ngáº¯n nháº¥t, nhÆ°ng Æ°u tiĂªn Ä‘i vá» phĂ­a
     * goal nĂªn nhanh hÆ¡n.
     * 
     * @return List cĂ¡c Ä‘iá»ƒm Vector3 trong tháº¿ giá»›i tháº­t
     *         Quy trĂ¬nh gá»“m:
     *         - Má»Ÿ danh sĂ¡ch (Open List): Chá»©a cĂ¡c Ä‘iá»ƒm Ä‘ang xem xĂ©t,
     *         Æ°u tiĂªn cĂ¡c
     *         Ä‘iá»ƒm cĂ³ tá»•ng chi phĂ­ (quĂ£ng Ä‘Æ°á»ng Ä‘Ă£ Ä‘i + khoáº£ng
     *         cĂ¡ch dá»± kiáº¿n tá»›i
     *         Ä‘Ă­ch) tháº¥p nháº¥t.
     *         - HĂ m Heuristic: DĂ¹ng khoáº£ng cĂ¡ch chim bay Ä‘á»ƒ Æ°á»›c lÆ°á»£ng
     *         quĂ£ng Ä‘Æ°á»ng
     *         cĂ²n
     *         láº¡i, giĂºp thuáº­t toĂ¡n tĂ¬m kiáº¿m cá»±c nhanh vĂ  Ä‘i Ä‘Ăºng
     *         hÆ°á»›ng.
     *         - TrĂ¡nh váº­t cáº£n: Thuáº­t toĂ¡n chá»‰ duyá»‡t qua cĂ¡c Ă´
     *         Ä‘Æ°á»£c Ä‘Ă¡nh dáº¥u lĂ 
     *         ROAD,
     *         hoĂ n toĂ n lá» Ä‘i cĂ¡c Ă´ lĂ  tĂ²a nhĂ  hay cĂ¢y cá»‘i.
     */
    private final ThreadLocal<Node[]> localNodesCache = new ThreadLocal<>();
    private final ThreadLocal<Integer> localSearchId = ThreadLocal.withInitial(() -> 0);

    /**
     * @param startX,  startZ: Tá»a Ä‘á»™ Ä‘iá»ƒm báº¯t Ä‘áº§u (World)
     * @param targetX, targetZ: Tá»a Ä‘á»™ Ä‘iá»ƒm Ä‘Ă­ch (World)
     * @return Danh sĂ¡ch cĂ¡c Vector3 mĂ´ táº£ Ä‘Æ°á»ng Ä‘i tá»« Start Ä‘áº¿n
     *         Goal. Tráº£ vá» null
     *         náº¿u khĂ´ng tĂ¬m tháº¥y Ä‘Æ°á»ng.
     */
    public List<Vector3> findPath(float startX, float startZ, float targetX, float targetZ) {
        GridPos start = findNearestRoad(startX, startZ, 1500);
        GridPos goal = findNearestRoad(targetX, targetZ, 1500);

        if (start == null || goal == null) {
            System.out.println("[NAV] Pathfinding failed: findNearestRoad returned null. Radius=1500");
            System.out.println("      Start World: (" + startX + ", " + startZ + ") -> Result=" + (start != null));
            System.out.println("      Goal World: (" + targetX + ", " + targetZ + ") -> Result=" + (goal != null));
            return null;
        }

        // Danh sĂ¡ch OpenSet chá»©a cĂ¡c Node Ä‘ang chá» xĂ©t, Æ°u tiĂªn Node cĂ³
        // fCost tháº¥p nháº¥t
        PriorityQueue<Node> openSet = new PriorityQueue<>();

        // Tá»I Æ¯U HIá»†U NÄ‚NG Cá»°C Äáº I: DĂ¹ng máº£ng cache 1 chiá»u, KHĂ”NG
        // Táº O Máº¢NG Má»I.
        // TrĂ¡nh táº¡o rĂ¡c bá»™ nhá»› (GC allocation) gĂ¢y Ä‘á»©ng game (Freeze)
        Node[] allNodes = localNodesCache.get();
        if (allNodes == null || allNodes.length < width * height) {
            allNodes = new Node[width * height];
            localNodesCache.set(allNodes);
        }

        // TÄƒng search ID Ä‘á»ƒ phĂ¢n biá»‡t cĂ¡c láº§n tĂ¬m kiáº¿m (TrĂ¡nh pháº£i
        // vĂ²ng láº·p reset máº£ng
        // cháº­m cháº¡p)
        int searchId = localSearchId.get() + 1;
        localSearchId.set(searchId);

        // Khá»Ÿi táº¡o Node báº¯t Ä‘áº§u
        int startIndex = start.z * width + start.x;
        Node startNode = allNodes[startIndex];
        if (startNode == null) {
            startNode = new Node(start.x, start.z);
            allNodes[startIndex] = startNode;
        }
        startNode.lastSearchId = searchId;
        startNode.gCost = 0; // Chi phĂ­ tá»« Ä‘iá»ƒm báº¯t Ä‘áº§u Ä‘áº¿n chĂ­nh nĂ³ lĂ  0
        startNode.hCost = heuristic(start.x, start.z, goal.x, goal.z); // Æ¯á»›c lÆ°á»£ng Ä‘Æ°á»ng chim bay tá»›i
                                                                       // Ä‘Ă­ch
        startNode.parent = null;
        startNode.closed = false;

        openSet.add(startNode);

        // 8 hÆ°á»›ng di chuyá»ƒn (ÄĂ´ng, TĂ¢y, Nam, Báº¯c vĂ  4 hÆ°á»›ng chĂ©o)
        int[][] dirs = { { 0, 1 }, { 0, -1 }, { 1, 0 }, { -1, 0 }, { 1, 1 }, { 1, -1 }, { -1, 1 }, { -1, -1 } };

        int maxSearchNodes = 100000; // Giá»›i háº¡n chá»‘ng treo mĂ¡y
        int nodesSearched = 0;

        while (!openSet.isEmpty() && nodesSearched < maxSearchNodes) {
            // Láº¥y Node tá»‘t nháº¥t hiá»‡n táº¡i ra khá»i hĂ ng Ä‘á»£i
            Node current = openSet.poll();

            // Tá»I Æ¯U HIá»†U NÄ‚NG Cá»°C Äáº I: PriorityQueue khĂ´ng cĂ³ hĂ m Update
            // Priority.
            // Ta cĂ³ thá»ƒ add nhiá»u báº£n sao cá»§a 1 Node vĂ o hĂ ng Ä‘á»£i. Báº£n sao
            // tá»‘t nháº¥t sáº½ ná»•i
            // lĂªn Ä‘áº§u.
            // Náº¿u láº¥y ra Node Ä‘Ă£ duyá»‡t (closed), ta bá» qua Ä‘á»ƒ trĂ¡nh láº·p
            // O(N) cá»§a hĂ m
            // openSet.contains()
            if (current.closed)
                continue;
            current.closed = true;

            nodesSearched++;

            // Náº¿u Ä‘Ă£ cháº¡m tá»›i Ä‘Ă­ch, tĂ¡i thiáº¿t láº­p Ä‘Æ°á»ng Ä‘i vĂ  tráº£
            // vá» káº¿t quáº£
            if (current.x == goal.x && current.z == goal.z) {
                return reconstructPath(current);
            }

            // Duyá»‡t qua cĂ¡c hĂ ng xĂ³m xung quanh
            for (int[] d : dirs) {
                int nx = current.x + d[0];
                int nz = current.z + d[1];

                // Kiá»ƒm tra xem hĂ ng xĂ³m cĂ³ náº±m trong map vĂ  cĂ³ pháº£i lĂ  Ä‘Æ°á»ng
                // (ROAD) khĂ´ng
                if (!isInBounds(nx, nz) || !isWalkable(nx, nz))
                    continue;

                int index = nz * width + nx;
                Node neighbor = allNodes[index];

                if (neighbor == null) {
                    neighbor = new Node(nx, nz);
                    neighbor.lastSearchId = searchId;
                    allNodes[index] = neighbor;
                } else if (neighbor.lastSearchId != searchId) {
                    neighbor.gCost = Float.MAX_VALUE;
                    neighbor.hCost = 0;
                    neighbor.parent = null;
                    neighbor.closed = false;
                    neighbor.lastSearchId = searchId;
                }

                // Náº¿u node nĂ y Ä‘Ă£ duyá»‡t xong, bá» qua
                if (neighbor.closed)
                    continue;

                // Chi phĂ­ di chuyá»ƒn: chĂ©o lĂ  1.414 (cÄƒn 2), tháº³ng lĂ  1.0
                float moveCost = (d[0] != 0 && d[1] != 0) ? 1.414f : 1.0f;
                float newGCost = current.gCost + moveCost;

                // Náº¿u tĂ¬m tháº¥y Ä‘Æ°á»ng Ä‘i tá»›i Node nĂ y vá»›i chi phĂ­ ráº» hÆ¡n
                if (newGCost < neighbor.gCost) {
                    neighbor.parent = current; // LÆ°u váº¿t Ä‘á»ƒ sau nĂ y quay ngÆ°á»£c láº¡i tĂ¬m Ä‘Æ°á»ng
                    neighbor.gCost = newGCost; // Cáº­p nháº­t chi phĂ­ thá»±c táº¿ Ä‘Ă£ Ä‘i
                    neighbor.hCost = heuristic(nx, nz, goal.x, goal.z); // Cáº­p nháº­t Æ°á»›c lÆ°á»£ng tá»›i Ä‘Ă­ch

                    // ThĂªm tháº³ng tháº³ng vĂ o PriorityQueue mĂ  khĂ´ng cáº§n dĂ¹ng
                    // openSet.contains (O(N)
                    // ráº¥t cháº­m)
                    openSet.add(neighbor);
                }
            }
        }

        return null; // Duyá»‡t háº¿t OpenSet mĂ  khĂ´ng tháº¥y Ä‘Ă­ch -> KhĂ´ng cĂ³ Ä‘Æ°á»ng
    }

    /**
     * HĂ m Æ°á»›c lÆ°á»£ng khoáº£ng cĂ¡ch chim bay (Euclidean distance) giá»¯a 2 Ă´
     * lÆ°á»›i.
     */
    private float heuristic(int x1, int z1, int x2, int z2) {
        float dx = x1 - x2;
        float dz = z1 - z2;
        return (float) Math.sqrt(dx * dx + dz * dz);
    }

    /**
     * Dá»±a vĂ o thuá»™c tĂ­nh 'parent' cá»§a tá»«ng Node, láº§n ngÆ°á»£c tá»«
     * Ä‘Ă­ch vá» Ä‘áº§u Ä‘á»ƒ láº¥y
     * chuá»—i tá»a Ä‘á»™.
     */
    private List<Vector3> reconstructPath(Node goalNode) {
        List<Vector3> path = new ArrayList<>();
        Node curr = goalNode;
        while (curr != null) {
            // Chuyá»ƒn tá»a Ä‘á»™ Ă´ lÆ°á»›i (Grid) vá» tá»a Ä‘á»™ thá»±c táº¿ (World)
            // Ä‘á»ƒ váº½
            path.add(0, new Vector3(gridToWorldX(curr.x), 0, gridToWorldZ(curr.z)));
            curr = curr.parent;
        }
        return path;
    }

    private static class Node implements Comparable<Node> {
        int x, z;
        float gCost = Float.MAX_VALUE;
        float hCost;
        Node parent;
        boolean closed = false; // ÄĂ¡nh dáº¥u Ä‘Ă£ chá»‘t chi phĂ­ ngáº¯n nháº¥t
        int lastSearchId = 0;

        Node(int x, int z) {
            this.x = x;
            this.z = z;
        }

        float fCost() {
            return gCost + hCost;
        }

        @Override
        public int compareTo(Node o) {
            return Float.compare(this.fCost(), o.fCost());
        }
    }

    public static class GridPos {
        public int x, z;

        public GridPos(int x, int z) {
            this.x = x;
            this.z = z;
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public float getGridSize() {
        return gridSize;
    }
}
