package fr.lezoo.stonks.api.event;

import fr.lezoo.stonks.player.PlayerData;
import fr.lezoo.stonks.share.Share;
import org.bukkit.event.HandlerList;

public class PlayerBuyShareEvent extends PlayerDataEvent {
    private final Share share;

    private static final HandlerList handlers = new HandlerList();

    /**
     * Called when a player buys a share from a certain stock
     *
     * @param playerData Player buying the share
     * @param share      Share bought
     */
    public PlayerBuyShareEvent(PlayerData playerData, Share share) {
        super(playerData);

        this.share = share;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
