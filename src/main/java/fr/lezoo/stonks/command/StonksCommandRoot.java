package fr.lezoo.stonks.command;

import fr.lezoo.stonks.command.nodes.ReloadTreeNode;
import fr.lezoo.stonks.command.nodes.SetTaxRateTreeNode;
import fr.lezoo.stonks.command.nodes.debug.DebugTreeNode;
import fr.lezoo.stonks.command.nodes.display.DisplayTreeNode;
import fr.lezoo.stonks.command.nodes.stock.StockTreeNode;
import fr.lezoo.stonks.command.objects.CommandTreeRoot;

public class StonksCommandRoot extends CommandTreeRoot {
    public StonksCommandRoot() {
        super("stonks", "stonks.admin");

        addChild(new StockTreeNode(this));
        addChild(new ReloadTreeNode(this));
        addChild(new DisplayTreeNode(this));
        addChild(new DebugTreeNode(this));
        addChild(new SetTaxRateTreeNode(this));
        // addChild(new ShareItemTreeNode(this));
    }
}
