package fr.lezoo.stonks.display.board;

import fr.lezoo.stonks.Stonks;
import fr.lezoo.stonks.quotation.Quotation;
import fr.lezoo.stonks.quotation.QuotationInfo;
import fr.lezoo.stonks.quotation.QuotationTimeDisplay;
import fr.lezoo.stonks.util.Utils;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ItemFrame;
import org.bukkit.util.Vector;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.UUID;

public class Board {
    private final UUID uuid;
    private final Quotation quotation;
    private final int height, width;
    private final Location location;

    /* private final List<>*/


    private final QuotationTimeDisplay time;

    private final BlockFace direction;

    public Board(Quotation quotation, int height, int width, Location location, QuotationTimeDisplay time, BlockFace direction) {
        uuid = UUID.randomUUID();
        this.quotation = quotation;
        this.height = height;
        this.width = width;
        this.location = location;
        this.time = time;
        this.direction = direction;
    }

    public Board(ConfigurationSection config) {
        uuid = UUID.fromString(config.getName());
        quotation = Stonks.plugin.quotationManager.get(config.getString("quotationid"));
        width = config.getInt("width");
        height = config.getInt("height");
        location = new Location(Bukkit.getWorld(config.getString("world")), config.getInt("x"), config.getInt("y"), config.getInt("z"));
        time = QuotationTimeDisplay.valueOf(config.getString("time").toUpperCase());
        direction = BlockFace.valueOf(config.getString("direction"));
    }

    public Quotation getQuotation() {
        return quotation;
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public Location getLocation() {
        return location;
    }

    public QuotationTimeDisplay getTime() {
        return time;
    }

    public BlockFace getDirection() {
        return direction;
    }

    public UUID getUuid() {
        return uuid;
    }

    /**
     * Saves the board into boarddata.yml
     */
    public void saveBoard(FileConfiguration boarddata) {
        boarddata.set(uuid.toString() + ".quotationid", quotation.getId());
        boarddata.set(uuid.toString() + ".width", width);
        boarddata.set(uuid.toString() + ".height", height);
        boarddata.set(uuid.toString() + ".x", location.getX());
        boarddata.set(uuid.toString() + ".y", location.getY());
        boarddata.set(uuid.toString() + ".z", location.getZ());
        boarddata.set(uuid.toString() + ".world", location.getWorld().getName());
        boarddata.set(uuid.toString() + ".time", time.toString().toLowerCase());
        boarddata.set(uuid.toString() + ".direction", direction.name());
    }

    public void destroy() {
        // We unregister the board
        Stonks.plugin.boardManager.removeBoard(uuid);
        // We destroy the entities
        Location newlocation = location.clone();
        // We create the wall to have the board with ItemFrames on it
        newlocation.add(0.5, 0.5, 0.5);
        // We get the direction to build horizontally and vertically
        Vector verticalBuildDirection = new Vector(0, 1, 0);
        Vector horizontalBuildDirection = direction.getDirection();

        // We need to clone to have deepmemory of it
        Vector horizontalLineReturn = horizontalBuildDirection.clone();
        horizontalLineReturn.multiply(-width);
        Vector itemFrameDirection = Utils.getItemFrameDirection(direction);

        // We get to the layer of ITemFrames
        newlocation.add(itemFrameDirection);
        // We remove them all
        for (int i = 0; i < height; i++) {
            // i stands for the line of the board and j the column
            for (int j = 0; j < width; j++) {
                // We remove the frame on the block
                newlocation.getWorld().getNearbyEntities(newlocation, 0.5, 0.5, 0.5, entity -> entity instanceof ItemFrame)
                        .forEach(itemFrame -> itemFrame.remove());
                newlocation.add(horizontalBuildDirection);
            }
            newlocation.add(verticalBuildDirection);
            newlocation.add(horizontalLineReturn);
        }
    }

    /**
     * Refreshes the board
     */
    public void refreshBoard() {
        // We use the createQuotationBoard method and say that it has already been created so we dont register it
        quotation.createQuotationBoard(true, location, direction, time, width, height);
    }

    public BufferedImage getImage(QuotationTimeDisplay time, int BOARD_WIDTH, int BOARD_HEIGHT) {



        // There is 128 pixel for each map
        BOARD_HEIGHT = 128 * BOARD_HEIGHT;
        BOARD_WIDTH = 128 * BOARD_WIDTH;

        // If not enough data on quotation data we take care of avoiding IndexOutOfBounds
        BufferedImage image = new BufferedImage(BOARD_WIDTH, BOARD_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = (Graphics2D) image.getGraphics();
        List<QuotationInfo> quotationData = quotation.getData(time);
        //If the quotation is Empty we print an error
        Validate.isTrue(quotationData.size()!=0,"The quotation : "+quotation.getId()+" has no values!!");

        int data_taken = Math.min(Stonks.plugin.configManager.quotationDataNumber, quotationData.size());

        int index = quotationData.size() - data_taken;

        // We look at the lowest val in the time we look backward to set the scale
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

        g2d.setStroke(new BasicStroke(3.0f));
        g2d.setColor(new Color(126, 51, 0));
        g2d.draw(new Rectangle2D.Double(0, 0.2 * BOARD_HEIGHT, BOARD_WIDTH * 0.8, 0.8 * BOARD_HEIGHT));
        g2d.draw(new Line2D.Double(0.8 * BOARD_WIDTH, 0.2 * BOARD_HEIGHT, 0.8 * BOARD_WIDTH, 0));
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font(null, Font.BOLD, BOARD_HEIGHT * 3 / 128));
        // We want only 2 numbers after the comma
        g2d.drawString(Stonks.plugin.configManager.currentPriceText +" : " +(double) ((int) (quotationData.get(quotationData.size() - 1).getPrice() * 100) / 1) / 100, (int) (0.03 * BOARD_WIDTH), (int) (0.04 * BOARD_HEIGHT));
        g2d.drawString(Stonks.plugin.configManager.highestPriceText+" : " + (double) ((int) (maxVal * 100) / 1) / 100, (int) (0.03 * BOARD_WIDTH), (int) (0.08 * BOARD_HEIGHT));
        g2d.drawString(Stonks.plugin.configManager.lowestPriceText+" : " + (double) ((int) (minVal * 100) / 1) / 100, (int) (0.03 * BOARD_WIDTH), (int) (0.12 * BOARD_HEIGHT));
        g2d.drawString(Stonks.plugin.configManager.evolutionText+" : "+quotation.getEvolution(time)+"%",(int) (0.03 * BOARD_WIDTH), (int) (0.16 * BOARD_HEIGHT));


        g2d.drawString(Stonks.plugin.configManager.companyNameText+" : " + quotation.getCompanyName(), (int) (0.35 * BOARD_WIDTH), (int) (0.04 * BOARD_HEIGHT));
        g2d.drawString(Stonks.plugin.configManager.stockNameText+" : " +quotation.getStockName(),(int) (0.35 * BOARD_WIDTH), (int) (0.08 * BOARD_HEIGHT));
        g2d.drawString(Stonks.plugin.configManager.timeVisualizedText+" : " +time.toString().toLowerCase(),(int) (0.35 * BOARD_WIDTH), (int) (0.12 * BOARD_HEIGHT));
        g2d.drawString(Stonks.plugin.configManager.quotationTypeText+" : ",(int) (0.35 * BOARD_WIDTH), (int) (0.16 * BOARD_HEIGHT));

        g2d.setColor(new Color(80, 30, 0));
        // Bouton SELL,SHORT,BUY,SET LEVERAGE
        // 0.82*BOARD_WIDTH to 0.98
        g2d.draw(new Rectangle2D.Double(0.82 * BOARD_WIDTH, 0.02 * BOARD_HEIGHT, 0.16 * BOARD_WIDTH, 0.19 * BOARD_HEIGHT));
        g2d.draw(new Rectangle2D.Double(0.82 * BOARD_WIDTH, 0.25 * BOARD_HEIGHT, 0.16 * BOARD_WIDTH, 0.2 * BOARD_HEIGHT));
        g2d.draw(new Rectangle2D.Double(0.82 * BOARD_WIDTH, 0.5 * BOARD_HEIGHT, 0.16 * BOARD_WIDTH, 0.2 * BOARD_HEIGHT));
        g2d.draw(new Rectangle2D.Double(0.82 * BOARD_WIDTH, 0.75 * BOARD_HEIGHT, 0.16 * BOARD_WIDTH, 0.2 * BOARD_HEIGHT));
        g2d.setColor(Color.GRAY);
        g2d.setFont(new Font(null, Font.BOLD, (int)(BOARD_HEIGHT * 3.5 / 128)));
        g2d.drawString("Leverage", (int) (0.83 * BOARD_WIDTH), (int) (0.1 * BOARD_HEIGHT));
        g2d.setFont(new Font(null, Font.BOLD, BOARD_HEIGHT * 4 / 128));
        g2d.drawString("BUY", (int) (0.83 * BOARD_WIDTH), (int) (0.35 * BOARD_HEIGHT));
        g2d.drawString("SHORT", (int) (0.83 * BOARD_WIDTH), (int) (0.60 * BOARD_HEIGHT));
        g2d.drawString("ORDERS", (int) (0.83 * BOARD_WIDTH), (int) (0.85 * BOARD_HEIGHT));

        g2d.setColor(Color.RED);
        Path2D.Double curve = new Path2D.Double();
        // If price = maxVal y =0.25 IMAGE_SIZE
        // If price = min Val y=0.95*IMAGE_SIZE (BOTTOM)
        double x = 0;
        double y = 0.95*BOARD_HEIGHT - (0.7 * BOARD_HEIGHT * (quotationData.get(index).getPrice() - minVal) / (maxVal - minVal));
        curve.moveTo(x, y);
        for (int i = 1; i < data_taken; i++) {
            // if data_taken < NUMBER_DATA,the graphics will be on the left of the screen mainly
            x = i * BOARD_WIDTH * 0.8 / Stonks.plugin.configManager.quotationDataNumber;
            y = 0.95*BOARD_HEIGHT - (0.7 * BOARD_HEIGHT * (quotationData.get(index + i).getPrice() - minVal) / (maxVal - minVal));
            curve.lineTo(x, y);
        }

        g2d.draw(curve);
        return image;
    }
}


