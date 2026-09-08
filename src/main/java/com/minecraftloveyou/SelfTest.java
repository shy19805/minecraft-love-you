package com.minecraftloveyou;

import com.minecraftloveyou.core.ModuleManager;
import com.minecraftloveyou.module.graves.GravesModule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

/** 服务端自检（仅当环境变量 WLY_SELFTEST=1 时触发）：逐项断言并打印 PASS/FAIL。 */
public final class SelfTest {

    private SelfTest() {
    }

    public static void run(MinecraftServer server) {
        List<String> out = new ArrayList<>();
        int pass = 0;
        int fail = 0;

        // T1 模块数量
        int mc = ModuleManager.ids().size();
        if (mc >= 35) {
            pass++;
            out.add("PASS T1 模块数量=" + mc);
        } else {
            fail++;
            out.add("FAIL T1 模块数量=" + mc);
        }

        // T2 大堆叠：普通物品 999
        ItemStack spruce = new ItemStack(Items.SPRUCE_LOG, 1);
        if (spruce.getMaxStackSize() == 999) {
            pass++;
            out.add("PASS T2 普通物品堆叠上限=999（实测 " + spruce.getMaxStackSize() + "）");
        } else {
            fail++;
            out.add("FAIL T2 普通物品堆叠上限=" + spruce.getMaxStackSize() + "（期望 999）");
        }

        // T3 耐久物品不可堆
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD, 1);
        if (sword.getMaxStackSize() <= 1) {
            pass++;
            out.add("PASS T3 耐久工具不可堆（实测 " + sword.getMaxStackSize() + "）");
        } else {
            fail++;
            out.add("FAIL T3 耐久工具上限=" + sword.getMaxStackSize() + "（期望 1）");
        }

        // T4 容器层 999
        SimpleContainer cont = new SimpleContainer(27);
        if (cont.getMaxStackSize() == 999) {
            pass++;
            out.add("PASS T4 容器堆叠上限=999");
        } else {
            fail++;
            out.add("FAIL T4 容器上限=" + cont.getMaxStackSize() + "（期望 999）");
        }

        // T5 容器+物品组合
        if (cont.getMaxStackSize(spruce) == 999) {
            pass++;
            out.add("PASS T5 容器×物品组合上限=999");
        } else {
            fail++;
            out.add("FAIL T5 组合上限=" + cont.getMaxStackSize(spruce) + "（期望 999）");
        }

        // T6 CODEC 放宽（此前的踢线点：count 999 重编码须通过）
        try {
            RegistryAccess ra = server.overworld().registryAccess();
            var ops = net.minecraft.resources.RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE, ra);
            var res = ItemStack.CODEC.encodeStart(ops, new ItemStack(Items.COBBLESTONE, 999));
            if (res.result().isPresent()) {
                pass++;
                out.add("PASS T6 CODEC 重编码 count=999 通过（不会踢线）");
            } else {
                fail++;
                out.add("FAIL T6 CODEC 重编码失败：" + res.error().orElse(null));
            }
        } catch (Throwable t) {
            fail++;
            out.add("FAIL T6 CODEC 异常：" + t);
        }

        // T7 墓碑注册 + 不遮挡面（noOcclusion 修复）
        try {
            var block = BuiltInRegistries.BLOCK.get(GravesModule.GRAVE_ID).map(net.minecraft.core.Holder.Reference::value).orElse(null);
            if (block == null) {
                fail++;
                out.add("FAIL T7 墓碑未注册");
            } else {
                var state = block.defaultBlockState();
                BlockPos pos = new BlockPos(0, 80, 0);
                // 26.2：noOcclusion 只关 canOcclude（该开关负责邻块面剔除修复）；isViewBlocking 不受影响
                boolean occludes = state.canOcclude();
                if (!occludes) {
                    pass++;
                    out.add("PASS T7 墓碑 canOcclude=false（邻块面不剔除，透视修复生效）");
                } else {
                    fail++;
                    out.add("FAIL T7 墓碑 canOcclude=" + occludes);
                }
            }
        } catch (Throwable t) {
            fail++;
            out.add("FAIL T7 墓碑检查异常：" + t);
        }

        // T8 bigstacktest 调试命令存在
        boolean cmd = server.getCommands().getDispatcher().getRoot().getChild("bigstacktest") != null;
        if (cmd) {
            pass++;
            out.add("PASS T8 /bigstacktest 命令已注册");
        } else {
            fail++;
            out.add("FAIL T8 /bigstacktest 命令缺失");
        }

        // T9 ctm 玻璃无缝纹理资源存在
        boolean tex = SelfTest.class.getClassLoader().getResource("assets/minecraft/textures/block/glass.png") != null;
        if (tex) {
            pass++;
            out.add("PASS T9 ctm 玻璃无缝纹理资源存在");
        } else {
            fail++;
            out.add("FAIL T9 ctm 玻璃纹理缺失");
        }

        // T10 语言文件：zh_cn 必备键 + en_us 前缀/键集对齐
        try (var zin = SelfTest.class.getClassLoader().getResourceAsStream("assets/minecraft_love_you/lang/zh_cn.json");
             var ein = SelfTest.class.getClassLoader().getResourceAsStream("assets/minecraft_love_you/lang/en_us.json")) {
            String z = zin != null ? new String(zin.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8) : "";
            String e = ein != null ? new String(ein.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8) : "";
            boolean ok = z.contains("key.minecraft_love_you.sort") && z.contains("key.minecraft_love_you.harvest")
                    && e.contains("\"wly.module.big-stacks\"") && e.contains("\"wly.module.anvil-master\"")
                    && e.contains("\"wly.module.eztweak-farmland-protect\"");
            if (ok) {
                pass++;
                out.add("PASS T10 语言文件条目存在（zh 必备键 + en_us 键集对齐）");
            } else {
                fail++;
                out.add("FAIL T10 语言文件条目异常");
            }
        } catch (Throwable t) {
            fail++;
            out.add("FAIL T10 语言文件异常：" + t);
        }

        // T11 powershot 新字段接线（初速/伤害开关/上限）
        double pv = com.minecraftloveyou.module.powershot.PowershotModule.powerVelocity;
        boolean dmg = com.minecraftloveyou.module.powershot.PowershotModule.damageEnabled;
        int dmx = com.minecraftloveyou.module.powershot.PowershotModule.damageMax;
        if (pv >= 0 && pv <= 10 && !dmg && dmx == 50) {
            pass++;
            out.add("PASS T11 powershot 接线（初速=" + pv + " 伤害=" + dmg + " 上限=" + dmx + "）");
        } else {
            fail++;
            out.add("FAIL T11 powershot 接线（初速=" + pv + " 伤害=" + dmg + " 上限=" + dmx + "）");
        }

        // T12 投掷物伤害运行时测试：雪球 onHitEntity 反射调用，验证血量下降（证明 mixin 生效）
        try {
            var level = server.overworld();
            var zombieType = BuiltInRegistries.ENTITY_TYPE
                    .get(net.minecraft.resources.Identifier.withDefaultNamespace("zombie"))
                    .map(net.minecraft.core.Holder.Reference::value).orElse(null);
            net.minecraft.world.entity.Entity zombie = zombieType == null ? null
                    : zombieType.create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
            if (!(zombie instanceof net.minecraft.world.entity.LivingEntity le)) {
                fail++;
                out.add("FAIL T12 无法创建测试僵尸");
            } else {
                le.setPos(0, 80, 0);
                level.addFreshEntity(le);
                double oldPv = com.minecraftloveyou.module.powershot.PowershotModule.powerVelocity;
                boolean oldDmg = com.minecraftloveyou.module.powershot.PowershotModule.damageEnabled;
                int oldDmx = com.minecraftloveyou.module.powershot.PowershotModule.damageMax;
                com.minecraftloveyou.module.powershot.PowershotModule.powerVelocity = 2.0;
                com.minecraftloveyou.module.powershot.PowershotModule.damageEnabled = true;
                com.minecraftloveyou.module.powershot.PowershotModule.damageMax = 50;
                try {
                    var snowball = new net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball(level, 0.5, 81, 0.5, net.minecraft.world.item.ItemStack.EMPTY);
                    snowball.setPos(0.5, 81, 0.5);
                    float hpBefore = le.getHealth();
                    var hit = new net.minecraft.world.phys.EntityHitResult(le, le.position());
                    var m = net.minecraft.world.entity.projectile.Projectile.class.getDeclaredMethod("onHitEntity", net.minecraft.world.phys.EntityHitResult.class);
                    m.setAccessible(true);
                    m.invoke(snowball, hit);
                    float hpAfter = le.getHealth();
                    float dealt = hpBefore - hpAfter;
                    // v=2 → dmg=round(2*5)=10
                    if (dealt >= 5 && dealt <= 15) {
                        pass++;
                        out.add("PASS T12 投掷物伤害生效（僵尸掉血 " + dealt + "，期望约10）");
                    } else {
                        fail++;
                        out.add("FAIL T12 伤害未按预期（掉血 " + dealt + "，期望约10）");
                    }
                } finally {
                    le.discard();
                    com.minecraftloveyou.module.powershot.PowershotModule.powerVelocity = oldPv;
                    com.minecraftloveyou.module.powershot.PowershotModule.damageEnabled = oldDmg;
                    com.minecraftloveyou.module.powershot.PowershotModule.damageMax = oldDmx;
                }
            }
        } catch (Throwable t) {
            fail++;
            out.add("FAIL T12 伤害测试异常：" + t);
        }

        WorldLovesYouMod.LOGGER.info("[我的世界爱你][自检] ===== 自检结果 {} PASS / {} FAIL =====", pass, fail);
        for (String line : out) {
            WorldLovesYouMod.LOGGER.info("[我的世界爱你][自检] " + line);
        }
    }
}