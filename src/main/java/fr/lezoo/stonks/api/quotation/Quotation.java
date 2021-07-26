package fr.lezoo.stonks.api.quotation;


import fr.lezoo.stonks.Stonks;
import fr.lezoo.stonks.api.NBTItem;
import fr.lezoo.stonks.api.util.Utils;
import fr.lezoo.stonks.version.ItemTag;
import org.apache.commons.lang.Validate;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.awt.*;
import java.awt.Color;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

/**
 * Place where players can buy and sell shares
 */
public class Quotation {
    private final String id, companyName, stockName;
    private List<QuotationInfo> quotationData = new ArrayList<>();
    // Refresh time of the quotation in milliseconds
    private final static int REFRESH_TIME = 1000;

    /**
     * Current price, not final because it is updated every so often
     */
    private double price;

    public Quotation(String id, String companyName, String stockName, List<QuotationInfo> quotationData) {
        this.id = id.toLowerCase().replace("_", "-").replace(" ", "-");
        this.companyName = companyName;
        this.stockName = stockName;
        this.quotationData = quotationData;
    }

    public Quotation(ConfigurationSection config) {
        this.id = config.getName();
        this.companyName = config.getString("company-name");
        this.stockName = config.getString("stock-name");
        // TODO
    }


    public Quotation(String id, String companyName, String stockName) {
        this.id = id;
        this.companyName = companyName;
        this.stockName = stockName;
    }

    public String getId() {
        return id;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getStockName() {
        return stockName;
    }

    public double getPrice() {
        return price;
    }

    public List<QuotationInfo> getQuotationData() {
        return quotationData;
    }


    /**
     * @param timeOut The delay in the past in millis on which the lowest value
     *                is calculated. For instance, if set to 1000 * 60 * 60 * 24,
     *                lowest value will be calculated over the last day.
     *                <p>
     *                For instance, QuotationInfo.WEEK_TIME_OUT or MONTH_TIME_OUT
     * @return Lowest value for the last X millis
     */
    public double getLowest(long timeOut) {
        double lowest = Double.MAX_VALUE;

        // Iterate through the list revert
        int k = quotationData.size() - 1;
        while (k >= 0) {
            QuotationInfo data = quotationData.get(k);
            if (System.currentTimeMillis() - data.getTimeStamp() > timeOut)
                break;

            if (data.getPrice() < lowest)
                lowest = data.getPrice();
            k--;
        }

        return lowest;
    }

    /**
     * @param timeOut The delay in the past in millis on which the highest value
     *                is calculated. For instance, if set to 1000 * 60 * 60 * 24,
     *                highest value will be calculated over the last day.
     *                <p>
     *                For instance, QuotationInfo.WEEK_TIME_OUT or MONTH_TIME_OUT
     * @return Highest value for the last X millis
     */
    public double getHighest(long timeOut) {
        double highest = Double.MIN_VALUE;

        // Iterate through the list revert
        int k = quotationData.size() - 1;
        while (k >= 0) {
            QuotationInfo data = quotationData.get(k);
            if (System.currentTimeMillis() - data.getTimeStamp() > timeOut)
                break;

            if (data.getPrice() > highest)
                highest = data.getPrice();
            k--;
        }

        return highest;
    }

    /**
     * This method compares the current price with the info which time
     * stamp matches the most the time stamp given as parameter
     * <p>
     * TODO use linear interpolation instead
     *
     * @param delta Difference of time in the past, in millis
     * @return Growth rate compared to some time ago
     */
    public double getEvolution(long delta) {
        QuotationInfo info = getClosestInfo(System.currentTimeMillis() - delta);

        // Check if no division by zero?
        double growthRate = 100 * Math.abs((getPrice() - info.getPrice()) / info.getPrice());

        return Utils.truncate(growthRate, 1);
    }

    /**
     * @return Quotation info with closest time
     * stamp to the one given as parameter
     */
    private QuotationInfo getClosestInfo(long timeStamp) {
        Validate.isTrue(!quotationData.isEmpty(), "Quotation data is empty");

        QuotationInfo closest = null;
        long delta = Long.MAX_VALUE;

        for (QuotationInfo info : quotationData)
            if (Math.abs(info.getTimeStamp() - timeStamp) < delta) {
                delta = Math.abs(info.getTimeStamp() - timeStamp);
                closest = info;
            }

        return closest;
    }


    public BufferedImage getQuotationBoardImage(int NUMBER_DATA, int BOARD_WIDTH, int BOARD_HEIGHT) {
        //There is 128 pixel for each map
        BOARD_HEIGHT = 128 * BOARD_HEIGHT;
        BOARD_WIDTH = 128 * BOARD_WIDTH;
        //If not enough data on quotation data we take care of avoiding IndexOutOfBounds
        BufferedImage image = new BufferedImage(BOARD_WIDTH, BOARD_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = (Graphics2D) image.getGraphics();
        int data_taken = Math.min(NUMBER_DATA, quotationData.size());
        int index = quotationData.size() - data_taken;

        //We look at the lowest val in the time we look backward to set the scale
        double minVal = quotationData.get(index).getPrice();
        double maxVal = quotationData.get(index).getPrice();
        for (int i = 1; i < data_taken; i++) {
            if (quotationData.get(index + i).getPrice() > maxVal)
                maxVal = quotationData.get(index + i).getPrice();
            if (quotationData.get(index + i).getPrice() < minVal)
                minVal = quotationData.get(index + i).getPrice();
        }

        // White background
        g2d.setColor(Color.WHITE);
        g2d.fill(new Rectangle2D.Double(2, 2, BOARD_WIDTH - 4, BOARD_HEIGHT - 4));

        g2d.setStroke(new BasicStroke(5.0f));
        g2d.setColor(new Color(126, 51, 0));
        g2d.draw(new Rectangle2D.Double(0, 0.2 * BOARD_HEIGHT, BOARD_WIDTH * 0.8, 0.8 * BOARD_HEIGHT));
        g2d.draw(new Line2D.Double(0.8 * BOARD_WIDTH, 0.2 * BOARD_HEIGHT, 0.8 * BOARD_WIDTH, 0));
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font(null, Font.PLAIN, BOARD_HEIGHT * 5 / 128));
        g2d.drawString("Company name : " + companyName, (int) (0.1 * BOARD_WIDTH), (int) (0.04 * BOARD_HEIGHT));
        //We want only 2 numbers after the comma
        g2d.drawString("Current Price : " + (double) ((int) (quotationData.get(quotationData.size() - 1).getPrice() * 100) / 1) / 100, (int) (0.1 * BOARD_WIDTH), (int) (0.08 * BOARD_HEIGHT));
        g2d.drawString("Highest Price : " + (double) ((int) (maxVal * 100) / 1) / 100, (int) (0.1 * BOARD_WIDTH), (int) (0.12 * BOARD_HEIGHT));
        g2d.drawString("Lowest Price : " + (double) ((int) (minVal * 100) / 1) / 100, (int) (0.1 * BOARD_WIDTH), (int) (0.16 * BOARD_HEIGHT));


        g2d.setColor(new Color(80, 30, 0));
        //Bouton SELL,SHORT,BUY,SET LEVERAGE
        //0.82*BOARD_WIDTH to 0.98
        g2d.draw(new Rectangle2D.Double(0.82 * BOARD_WIDTH, 0.02 * BOARD_HEIGHT, 0.18 * BOARD_WIDTH, 0.19 * BOARD_HEIGHT));
        g2d.draw(new Rectangle2D.Double(0.82 * BOARD_WIDTH, 0.25 * BOARD_HEIGHT, 0.18 * BOARD_WIDTH, 0.2 * BOARD_HEIGHT));
        g2d.draw(new Rectangle2D.Double(0.82 * BOARD_WIDTH, 0.5 * BOARD_HEIGHT, 0.18 * BOARD_WIDTH, 0.2 * BOARD_HEIGHT));
        g2d.draw(new Rectangle2D.Double(0.82 * BOARD_WIDTH, 0.75 * BOARD_HEIGHT, 0.18 * BOARD_WIDTH, 0.2 * BOARD_HEIGHT));
        g2d.setColor(Color.GRAY);
        g2d.setFont(new Font(null, Font.BOLD, BOARD_HEIGHT * 5 / 128));
        g2d.drawString("Set Leverage", (int) (0.84 * BOARD_WIDTH), (int) (0.1 * BOARD_HEIGHT));
        g2d.setFont(new Font(null, Font.BOLD, BOARD_HEIGHT * 8 / 128));
        g2d.drawString("BUY", (int) (0.84 * BOARD_WIDTH), (int) (0.35 * BOARD_HEIGHT));
        g2d.drawString("SHORT", (int) (0.84 * BOARD_WIDTH), (int) (0.60 * BOARD_HEIGHT));
        g2d.drawString("SELL", (int) (0.84 * BOARD_WIDTH), (int) (0.85 * BOARD_HEIGHT));


        g2d.setColor(Color.RED);
        Path2D.Double curve = new Path2D.Double();
        //If price = maxVal y =0.2 IMAGE_SIZE
        //If price = min Val y=IMAGE_SIZE (BOTTOM)
        double x = 0;
        double y = BOARD_HEIGHT - (0.8 * BOARD_HEIGHT * (quotationData.get(index).getPrice() - minVal) / (maxVal - minVal));
        curve.moveTo(x, y);
        for (int i = 1; i < data_taken; i++) {
            //if data_taken < NUMBER_DATA,the graphics will be on the left of the screen mainly
            x = i * BOARD_WIDTH * 0.8 / NUMBER_DATA;
            y = BOARD_HEIGHT - (0.8 * BOARD_HEIGHT * (quotationData.get(index + i).getPrice() - minVal) / (maxVal - minVal));
            curve.lineTo(x, y);
        }
        g2d.draw(curve);
        return image;
    }


    public Vector getItemFrameDirection(BlockFace blockFace) {
        //the direction that we will move in to put item frames
        Vector itemFrameDirection = new Vector();

        switch (blockFace) {
            case NORTH:
                itemFrameDirection = BlockFace.EAST.getDirection();
                break;
            case EAST:
                itemFrameDirection = BlockFace.SOUTH.getDirection();
                break;
            case SOUTH:
                itemFrameDirection = BlockFace.WEST.getDirection();
                break;
            case WEST:
                itemFrameDirection = BlockFace.NORTH.getDirection();
                break;
        }
        return itemFrameDirection;
    }


    /**
     * Creates a 5x5 map of the Quotation to the player
     * gives the player all the maps in his inventory
     */
    public void createQuotationBoard(boolean hasBeenCreated, Location initiallocation, BlockFace blockFace, int NUMBER_DATA, int BOARD_WIDTH, int BOARD_HEIGHT) {
        BufferedImage image = getQuotationBoardImage(NUMBER_DATA, BOARD_WIDTH, BOARD_HEIGHT);

        //We make sure to not chang the location given in argument
        Location location = initiallocation.clone();
        //We create the wall to have the board with ItemFrames on it
        location.add(0.5, 0.5, 0.5);
        //We get the direction to build horizontally and vertically
        Vector verticalBuildDirection = new Vector(0, 1, 0);
        Vector horizontalBuildDirection = blockFace.getDirection();

        //We need to clone to have deepmemory of it
        Vector horizontalLineReturn = horizontalBuildDirection.clone();
        horizontalLineReturn.multiply(-BOARD_WIDTH);


        Vector itemFrameDirection = getItemFrameDirection(blockFace);

        //if the board has never been created we register it
        if (!hasBeenCreated) {
            Stonks.plugin.boardManager.register(new Board(this, BOARD_HEIGHT, BOARD_WIDTH, initiallocation, NUMBER_DATA, blockFace));
        }

        //We get the board corresponding to the one we are creating or updating
        Board board = Stonks.plugin.boardManager.getBoard(initiallocation, blockFace);

        for (int i = 0; i < BOARD_HEIGHT; i++) {
            //i stands for the line of the board and j the column

            for (int j = 0; j < BOARD_WIDTH; j++) {

                if (!hasBeenCreated)
                    location.getBlock().setType(Material.DARK_OAK_WOOD);
                //We check if there is a block to build the frames on
                if (location.getBlock().getType().isBlock()) {

                    //We catch the IllegalArgumentException of spawnEntity method
                    try {
                        location.add(itemFrameDirection);
                        location.getWorld().getNearbyEntities(location, 0.5, 0.5, 0.5, entity -> entity instanceof ItemFrame).forEach(entity -> entity.remove());
                        //If there is a problem at onz point we stop entirely the creation of the board
                        ItemFrame itemFrame = (ItemFrame) location.getWorld().spawnEntity(location, EntityType.ITEM_FRAME);
                        //We create the map that will go in the itemframe
                        ItemStack mapItem = new ItemStack(Material.FILLED_MAP, 1);
                        MapMeta meta = (MapMeta) mapItem.getItemMeta();
                        MapView mapView = Bukkit.createMap(Bukkit.getWorld("world"));
                        mapView.getRenderers().clear();
                        //j=0 ->x=0 but i=0 ->i =BOARD_HEIGHT because of how graphics 2D works
                        mapView.addRenderer(new QuotationBoardRenderer(image.getSubimage(128 * j, 128 * (BOARD_HEIGHT - i - 1), 128, 128)));
                        mapView.setTrackingPosition(false);
                        mapView.setUnlimitedTracking(false);
                        meta.setMapView(mapView);
                        mapItem.setItemMeta(meta);
                        //We store the UUID of the board within each ItemFrame of the board
                        NBTItem nbtItem = NBTItem.get(mapItem);
                        ItemTag itemTag = new ItemTag("boarduuid", board.getUuid().toString());
                        nbtItem.addTag(itemTag);
                        mapItem = nbtItem.toItem();
                        itemFrame.setItem(mapItem);
                        location.subtract(itemFrameDirection);
                    } catch (IllegalArgumentException e) {
                        board.destroy();
                        e.printStackTrace();
                        return;
                    }

                }

                location.add(horizontalBuildDirection);
            }

            location.add(verticalBuildDirection);
            location.add(horizontalLineReturn);
        }


    }


    public static final String MAP_ITEM_TAG_PATH = "StonksQuotationMap";

    /**
     * @param NUMBER_DATA number of points taken for the graphic
     * @return a map where we can see the quotation
     */

    public ItemStack createQuotationMap(int NUMBER_DATA) {
        ItemStack mapItem = new ItemStack(Material.FILLED_MAP, 1);
        //We cast the ItemMeta into MapMeta
        MapMeta meta = (MapMeta) mapItem.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "StockPaper of :" + companyName);
        meta.setLore(Arrays.asList(ChatColor.BLUE + "Company name : " + companyName, ChatColor.GREEN + "Stock name : " + stockName));

        //Creates a mapview to later change its Renderer and load img
        MapView mapView = Bukkit.createMap(Bukkit.getWorld("world"));
        mapView.getRenderers().clear();
        mapView.addRenderer(new QuotationMapRenderer(this, NUMBER_DATA));
        meta.setMapView(mapView);
        mapItem.setItemMeta(meta);

        NBTItem nbtItem = NBTItem.get(mapItem);
        nbtItem.addTag(new ItemTag(MAP_ITEM_TAG_PATH, true));
        return nbtItem.toItem();
    }
}
