import random
from shared import *

redemptionId = "31702460-3cf4-4576-a299-fe712a1123d2"
env = "PLUGIN"


def run(cost, user_name, user, handler, player_pos, odds, redemption):
    if odds <= 100:#20:
        world = handler.getWorld()
        player = handler.getPlayer().getPlayer()

        pilliger = int((random.random() * 2)) + 2
        vindicators = int((random.random() * 2)) + 5
        witch = int((random.random() * 2)) + 1
        evoker = 2
        ravagerVindicator = int((random.random() * 1)) + 1
        ravagerEvoker = 1

        total = pilliger + vindicators + witch + evoker + ravagerEvoker + ravagerVindicator

        i = 0

        while i < total:
            pos = player_pos.clone().set(0, 0, 0)

            pos.setX(int(random.random() * ((player_pos.getX() + 30) - (player_pos.getX() - 30)) + (player_pos.getX() + 30)))
            pos.setZ(int(random.random() * ((player_pos.getZ() + 30) - (player_pos.getZ() - 30)) + (player_pos.getZ() + 30)))
            pos.setY(world.getMaxHeight())

            while world.isAirAt(pos) and pos.getY() > world.getMinHeight():
                pos.addY(-1)

            if pos.getY() == world.getMinHeight():
                pos.setY(player_pos.getY())

                for x in nums(pos.getX(), pos.getX()):
                    for z in nums(pos.getZ()-1, pos.getZ()+1):
                        tmp = pos.clone().set(x, pos.getY(), z)
                        if world.isAirAt(tmp):
                            world.setBlock(tmp, "DIRT")

            if pilliger > 0:
                e = world.spawnEntity(pos, "Pillager")
                e.setSilent(True)
                e.setTarget(player)
                handler.setName(user_name, e)
                pilliger -= 1
            elif vindicators > 0:
                e = world.spawnEntity(pos, "Vindicator")
                e.setSilent(True)
                e.setTarget(player)
                handler.setName(user_name, e)
                vindicators -= 1
            elif witch > 0:
                e = world.spawnEntity(pos, "witch")
                e.setSilent(True)
                e.setTarget(player)
                handler.setName(user_name, e)
                witch -= 1
            elif evoker > 0:
                e = world.spawnEntity(pos, "evoker")
                e.setSilent(True)
                e.setTarget(player)
                handler.setName(user_name, e)
                evoker -= 1
            elif ravagerEvoker > 0:
                rider = world.spawnEntity(pos, "Evoker")
                rider.setSilent(True)
                rider.setTarget(player)
                handler.setName(user_name, rider)

                e = world.spawnEntity(pos, "Ravager")
                e.setSilent(True)
                e.setTarget(player)
                e.addPassenger(rider)
                handler.setName(user_name, e)
                ravagerEvoker -= 1

            elif ravagerVindicator > 0:
                rider = world.spawnEntity(pos, "Vindicator")
                rider.setSilent(True)
                rider.setTarget(player)
                handler.setName(user_name, rider)

                e = world.spawnEntity(pos, "Ravager")
                e.setSilent(True)
                e.setTarget(player)
                e.addPassenger(rider)
                handler.setName(user_name, e)
                ravagerVindicator -= 1
