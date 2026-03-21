package com.futurecity.game.inputs;

/**
 * IInputController
 * <p>
 * Interface nĂ y trá»«u tÆ°á»£ng hĂ³a cĂ¡c lá»‡nh Ä‘iá» u khiá»ƒn.
 * GiĂºp tĂ¡ch biá»‡t logic game (MainCharactor) khá» i thiáº¿t bá»‹ Ä‘áº§u vĂ o
 * (BĂ n phĂ­m,
 * Tay cáº§m, MĂ n hĂ¬nh cáº£m á»©ng).
 */
public interface IInputController {

    /**
     * Láº¥y giĂ¡ trá»‹ Ä‘iá» u hÆ°á»›ng theo trá»¥c Ngang (X).
     * 
     * @return
     *         -1: Sang TrĂ¡i (Left)
     *         0: Ä á»©ng yĂªn
     *         +1: Sang Pháº£i (Right)
     */
    float getHorizontal();

    /**
     * Láº¥y giĂ¡ trá»‹ Ä‘iá»u hÆ°á»›ng theo trá»¥c Dá»c (Z).
     * 
     * @return
     *         -1: LĂ¹i láº¡i (Backward)
     *         0: Äá»©ng yĂªn
     *         +1: Tiáº¿n lĂªn (Forward)
     */
    float getVertical();

    /**
     * Kiá»ƒm tra xem ngÆ°á»i chÆ¡i cĂ³ Ä‘ang giá»¯ nĂºt CHáº Y hay khĂ´ng.
     * 
     * @return true náº¿u Ä‘ang giá»¯ (Shift/E).
     */
    boolean isRunPressed();

    /**
     * Kiá»ƒm tra xem ngÆ°á» i chÆ¡i cĂ³ báº¥m nĂºt NHáº¢Y hay khĂ´ng.
     * 
     * @return true náº¿u báº¥m nĂºt Jump (Space).
     */
    boolean isJumpPressed();

    /**
     * Kiá»ƒm tra xem nĂºt TÆ¯Æ NG TĂ C (Interact) cĂ³ vá»«a Ä‘Æ°á»£c báº¥m hay
     * khĂ´ng.
     * DĂ¹ng cho hĂ nh Ä‘á»™ng: Má»Ÿ cá»­a, Nháº·t Ä‘á»“, NĂ³i chuyá»‡n NPC.
     * 
     * @return true chá»‰ trong frame Ä‘áº§u tiĂªn khi phĂ­m Ä‘Æ°á»£c nháº¥n xuá»‘ng
     *         (Just
     *         Pressed).
     */
    boolean isInteractJustPressed();

    /**
     * Khá»Ÿi táº¡o láº¡i tráº¡ng thĂ¡i input (vá» 0).
     * Há»¯u Ă­ch khi Ä‘Ă³ng UI Ä‘á»ƒ trĂ¡nh káº¹t phĂ­m.
     */
    void clearInput();
}
