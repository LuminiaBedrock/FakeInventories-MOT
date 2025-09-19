package me.iwareq.fakeinventories;

import cn.nukkit.Player;
import cn.nukkit.block.BlockID;
import cn.nukkit.blockentity.BlockEntity;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.inventory.InventoryTransactionEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.event.server.DataPacketReceiveEvent;
import cn.nukkit.inventory.InventoryType;
import cn.nukkit.inventory.transaction.action.SlotChangeAction;
import cn.nukkit.item.Item;
import cn.nukkit.network.protocol.PacketViolationWarningPacket;
import cn.nukkit.network.protocol.ProtocolInfo;
import cn.nukkit.plugin.PluginBase;
import lombok.Getter;
import me.iwareq.fakeinventories.block.DoubleFakeBlock;
import me.iwareq.fakeinventories.block.FakeBlock;
import me.iwareq.fakeinventories.block.FakeBlockOffset;
import me.iwareq.fakeinventories.block.SingleFakeBlock;
import me.iwareq.fakeinventories.util.FakeInvDispatcher;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class FakeInventories extends PluginBase implements Listener {

    @Getter
    private static FakeInventories instance;

    private static final Map<Player, FakeInvDispatcher> FAKE_INV_DISPATCHERS = new HashMap<>();
    private static final Map<InventoryType, FakeBlock> FAKE_BLOCKS = new EnumMap<>(InventoryType.class);
    private static FakeBlockOffset FAKE_BLOCK_OFFSET;

    @Override
    public void onLoad() {
        this.saveDefaultConfig();
        instance = this;
        FAKE_BLOCKS.put(InventoryType.CHEST, new SingleFakeBlock(BlockID.CHEST, BlockEntity.CHEST));
        FAKE_BLOCKS.put(InventoryType.ENDER_CHEST, new SingleFakeBlock(BlockID.ENDER_CHEST, BlockEntity.ENDER_CHEST));
        FAKE_BLOCKS.put(InventoryType.DOUBLE_CHEST, new DoubleFakeBlock(BlockID.CHEST, BlockEntity.CHEST));
        FAKE_BLOCKS.put(InventoryType.FURNACE, new SingleFakeBlock(BlockID.FURNACE, BlockEntity.FURNACE));
        FAKE_BLOCKS.put(InventoryType.WORKBENCH, new SingleFakeBlock(BlockID.WORKBENCH, InventoryType.WORKBENCH.getDefaultTitle()));
        FAKE_BLOCKS.put(InventoryType.BREWING_STAND, new SingleFakeBlock(BlockID.BREWING_STAND_BLOCK, BlockEntity.BREWING_STAND));
        FAKE_BLOCKS.put(InventoryType.DISPENSER, new SingleFakeBlock(BlockID.DISPENSER, InventoryType.DISPENSER.getDefaultTitle()));
        FAKE_BLOCKS.put(InventoryType.DROPPER, new SingleFakeBlock(BlockID.DROPPER, InventoryType.DROPPER.getDefaultTitle()));
        FAKE_BLOCKS.put(InventoryType.HOPPER, new SingleFakeBlock(BlockID.HOPPER_BLOCK, BlockEntity.HOPPER));
        FAKE_BLOCKS.put(InventoryType.SHULKER_BOX, new SingleFakeBlock(BlockID.SHULKER_BOX, BlockEntity.SHULKER_BOX));
        FAKE_BLOCK_OFFSET = FakeBlockOffset.valueOf(this.getConfig().getString("fake-block-offset-mode").toUpperCase());
    }

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryTransaction(InventoryTransactionEvent event) {
        event.getTransaction().getActions().forEach(action -> {
            if (action instanceof SlotChangeAction slotChange) {
                if (slotChange.getInventory() instanceof FakeInventory inventory) {
                    int slot = slotChange.getSlot();
                    Item sourceItem = action.getSourceItem();
                    inventory.handle(slot, sourceItem, event);
                }
            }
        });
    }

    @EventHandler
    public void onPacketReceive(DataPacketReceiveEvent event) {
        if (event.getPacket() instanceof PacketViolationWarningPacket packet) {
            if (packet.packetId == ProtocolInfo.toNewProtocolID(PacketViolationWarningPacket.NETWORK_ID) &&
                    packet.type == PacketViolationWarningPacket.PacketViolationType.UNKNOWN &&
                    packet.severity == PacketViolationWarningPacket.PacketViolationSeverity.UNKNOWN
            ) {
                FakeInvDispatcher dispatcher = FakeInventories.getFakeInvDispatcher(event.getPlayer());
                if (dispatcher.isActive()) {
                    event.setCancelled();
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        FAKE_INV_DISPATCHERS.remove(event.getPlayer());
    }

    public static FakeInvDispatcher getFakeInvDispatcher(Player player) {
        return FAKE_INV_DISPATCHERS.computeIfAbsent(player, p -> new FakeInvDispatcher());
    }

    public static FakeBlock getFakeBlock(InventoryType inventoryType) {
        FakeBlock fakeBlock = FAKE_BLOCKS.get(inventoryType);
        if (fakeBlock == null) {
            throw new NullPointerException("FakeBlock for " + inventoryType.name() + " inventory not found!");
        }
        return fakeBlock;
    }

    public static FakeBlockOffset getFakeBlockOffset() {
        return FAKE_BLOCK_OFFSET;
    }
}
