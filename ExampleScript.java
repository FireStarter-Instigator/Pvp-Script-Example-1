package net.runelite.client.plugins.microbot.example;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.bee.MossKiller.WildyKillerScript;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.player.Rs2PlayerModel;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static net.runelite.api.gameval.ItemID.MAPLE_SHORTBOW;


public class ExampleScript extends Script {

    @Inject
    ExamplePlugin examplePlugin;

    @Inject
    WildyKillerScript wildyKillerScript;

    public static boolean test = false;
    private Rs2PlayerModel target = null; // usa "private" para claridad

    public boolean run(ExampleConfig config) {
        Microbot.enableAutoRunOn = false;

        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn()) return;
                if (!super.run()) return;

                Rs2PlayerModel myPlayer = Rs2Player.getLocalPlayer();
                List<Rs2PlayerModel> players = Rs2Player.getPlayersInCombatLevelRange();

                // Validamos si el target sigue en pantalla y no ha muerto
                if (target != null) {
                    boolean stillVisible = false;
                    for (Rs2PlayerModel player : players) {
                        if (player.getName() != null && player.getName().equals(target.getName())) {
                            stillVisible = true;
                            target = player; // refrescar la instancia actual
                            break;
                        }
                    }
                    if (!stillVisible || target.isDead()) {
                        target = null; // Se ha ido o ha muerto
                    }
                }

                // Si no hay target actual, buscar nuevo
                if (target == null) {
                    for (Rs2PlayerModel player : players) {
                        if (player == null || player.getName() == null) continue;

                        if (player.getInteracting() != null &&
                                player.getInteracting().getName().equals(myPlayer.getName())) {
                            target = player;
                            break;
                        }
                    }
                }

                if (target != null) {
                    if (myPlayer.getInteracting() == null ||
                            !myPlayer.getInteracting().getName().equals(target.getName())) {
                        if(getCombatStyle(target) == "RANGE") {
                            Rs2Inventory.equip(MAPLE_SHORTBOW);
                            Microbot.log("IDENTIFIED AS RANGED");
                        }
                        if(!ExamplePlugin.attackDelay) Rs2Player.attack(target);
                        Microbot.log("Atacando a " + target.getName());
                    } else {
                        Microbot.log("Ya estamos interactuando con " + target.getName());
                    }
                }

                Microbot.log("WE ARE AT THE END OF THE RUN");

            } catch (Exception ex) {
                System.out.println("Error en loop: " + ex.getMessage());
            }

        }, 0, 1000, TimeUnit.MILLISECONDS);

        return true;
    }

    public String getCombatStyle(Rs2PlayerModel player) {
        int[] equipmentIds = player.getPlayerComposition().getEquipmentIds();

        if (equipmentIds == null) {
            return "UNKNOWN";
        }

        int MAPLE_SHORTBOW_ID = 2901;
        int MAPLE_LONGBOW_ID = 2899;
        int RUNE_SCIMITAR_ID = 3381;
        int GILDED_SCIMITAR_ID = 14437;
        int STAFF_OF_FIRE_ID = 3435;
        int STAFF_OF_WATER_ID = 3431;
        int STAFF_OF_EARTH_ID = 3433;
        int STAFF_OF_AIR_ID = 3429;

        // Check the weapon slot (usually index 3 in equipment array)
        int weaponId = equipmentIds[3];

        // Check for Ranged weapon (Maple Shortbow)
        if (weaponId == MAPLE_SHORTBOW_ID || weaponId == MAPLE_LONGBOW_ID ) {
            return "RANGE";
        }

        // Check for Melee weapons (Rune Scimitar, etc.)
        if (weaponId == RUNE_SCIMITAR_ID || weaponId == GILDED_SCIMITAR_ID) {
            return "MELEE";
        }

        // Check for Magic weapons (Staff of Fire, etc.)
        if (weaponId == STAFF_OF_FIRE_ID || weaponId == STAFF_OF_WATER_ID || weaponId == STAFF_OF_EARTH_ID || weaponId == STAFF_OF_AIR_ID) {
            return "MAGIC";
        }

        return "UNKNOWN";
    }

    @Override
    public void shutdown() {
        target = null;
        super.shutdown();
    }
}