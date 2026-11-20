package fr.citizen.mancie.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import fr.citizen.mancie.network.CastSpellPayload;
import fr.citizen.mancie.spell.Spell;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Roulette des sorts : on maintient G, on vise un sort avec la souris, on relâche G pour le lancer.
 * Un clic gauche lance aussi le sort survolé. Échap ferme sans rien lancer.
 */
public class SpellWheelScreen extends Screen {
    private static final float INNER = 38f;
    private static final float OUTER = 100f;
    private static final double GAP = 0.025;

    private static final int GOLD = 0xFFC9A44C;
    private static final int DARK = 0xD8140F0C;

    private final List<Spell> spells = new ArrayList<>();
    private int hovered = -1;
    private boolean done = false;
    private int age = 0;
    /** Appui bref sur G : la roulette reste ouverte, on lance par clic ou nouvel appui sur G. */
    private boolean toggleMode = false;

    public SpellWheelScreen() {
        super(Component.literal("Roulette des sorts"));
        for (String id : ClientSpellData.spells()) {
            Spell s = Spell.byId(id);
            if (s != null) spells.add(s);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        // Si U est relâché (vérification directe du clavier), on lance le sort survolé.
        age++;
        if (minecraft == null || toggleMode) return;
        InputConstants.Key key = ClientEvents.ROULETTE.getKey();
        if (key.getType() == InputConstants.Type.KEYSYM
                && !InputConstants.isKeyDown(minecraft.getWindow().getWindow(), key.getValue())) {
            onRouletteReleased();
        }
    }

    private void onRouletteReleased() {
        if (toggleMode) return;
        if (age <= 6) toggleMode = true; // simple tapotement : on garde la roulette ouverte
        else castAndClose();
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (ClientEvents.ROULETTE.matches(keyCode, scanCode)) {
            onRouletteReleased();
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (toggleMode && ClientEvents.ROULETTE.matches(keyCode, scanCode)) {
            castAndClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            castAndClose();
            return true;
        }
        if (button == 1) {
            onClose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void castAndClose() {
        if (done) return;
        done = true;
        if (hovered >= 0 && hovered < spells.size()) {
            PacketDistributor.sendToServer(new CastSpellPayload(spells.get(hovered).id()));
        }
        onClose();
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0x50000000);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        if (spells.isEmpty()) {
            onClose();
            return;
        }

        float cx = width / 2f;
        float cy = height / 2f;
        int n = spells.size();
        double step = Math.PI * 2 / n;
        double start = -Math.PI / 2 - step / 2; // le sort n°1 est en haut

        // Sort survolé selon la direction de la souris
        double dx = mouseX - cx, dy = mouseY - cy;
        if (dx * dx + dy * dy >= (INNER * 0.5) * (INNER * 0.5)) {
            double a = Math.atan2(dy, dx) - start;
            a = ((a % (Math.PI * 2)) + Math.PI * 2) % (Math.PI * 2);
            hovered = Math.min(n - 1, (int) (a / step));
        } else {
            hovered = -1;
        }

        g.flush();

        // Cadre doré extérieur et intérieur
        arc(g, cx, cy, OUTER + 2, OUTER + 4, 0, Math.PI * 2, GOLD);
        arc(g, cx, cy, INNER - 4, INNER - 2, 0, Math.PI * 2, GOLD);
        arc(g, cx, cy, 0, INNER - 4, 0, Math.PI * 2, DARK);

        for (int i = 0; i < n; i++) {
            Spell s = spells.get(i);
            double a0 = start + i * step + GAP;
            double a1 = start + (i + 1) * step - GAP;
            boolean hov = i == hovered;
            boolean cd = ClientSpellData.remainingSeconds(s.id()) > 0;

            int fill = hov ? withAlpha(s.mancie().color(), cd ? 0x70 : 0xC8) : DARK;
            arc(g, cx, cy, INNER, OUTER, a0, a1, fill);
            // liseré de la couleur de la mancie
            arc(g, cx, cy, OUTER - 4, OUTER, a0, a1, cd ? 0xFF555555 : s.mancie().color());
        }

        // Symboles et recharges dans chaque case
        for (int i = 0; i < n; i++) {
            Spell s = spells.get(i);
            double mid = start + (i + 0.5) * step;
            float r = (INNER + OUTER) / 2f;
            float x = cx + (float) Math.cos(mid) * r;
            float y = cy + (float) Math.sin(mid) * r;
            int cd = ClientSpellData.remainingSeconds(s.id());

            g.pose().pushPose();
            g.pose().translate(x, y, 0);
            g.pose().scale(1.8f, 1.8f, 1f);
            String sym = s.mancie().symbol();
            int col = cd > 0 ? 0xFF777777 : (i == hovered ? 0xFFFFFFFF : s.mancie().color());
            g.drawString(font, sym, -font.width(sym) / 2, -4, col, false);
            g.pose().popPose();

            String label = cd > 0 ? cd + "s" : String.valueOf(i + 1);
            g.drawString(font, label, (int) x - font.width(label) / 2, (int) y + 10,
                    cd > 0 ? 0xFFAAAAAA : 0xFFE8DDBF, true);
        }

        // Titre
        String title = "✦ Roulette des sorts ✦";
        g.drawString(font, title, (int) cx - font.width(title) / 2, (int) (cy - OUTER - 18), GOLD, true);

        // Centre : symbole du sort survolé
        if (hovered >= 0) {
            Spell s = spells.get(hovered);
            g.pose().pushPose();
            g.pose().translate(cx, cy, 0);
            g.pose().scale(2.5f, 2.5f, 1f);
            g.drawString(font, s.mancie().symbol(), -font.width(s.mancie().symbol()) / 2, -4, s.mancie().color(), false);
            g.pose().popPose();

            int y = (int) (cy + OUTER + 12);
            drawCentered(g, s.displayName(), cx, y, s.mancie().color());
            int cd = ClientSpellData.remainingSeconds(s.id());
            String info = s.mancie().displayName() + (cd > 0 ? " · recharge " + cd + " s" : " · prêt");
            drawCentered(g, info, cx, y + 11, 0xFFB8AE95);
            drawCentered(g, s.description(), cx, y + 22, 0xFFE8DDBF);
        } else {
            drawCentered(g, "Vise un sort, relâche " + ClientEvents.ROULETTE.getTranslatedKeyMessage().getString() + " pour le lancer", cx, (int) (cy + OUTER + 12), 0xFFB8AE95);
        }
    }

    private void drawCentered(GuiGraphics g, String text, float cx, int y, int color) {
        g.drawString(font, text, (int) cx - font.width(text) / 2, y, color, true);
    }

    private static int withAlpha(int argb, int alpha) {
        return (alpha << 24) | (argb & 0xFFFFFF);
    }

    /** Dessine un anneau (ou disque si r1 = 0) entre les angles a0 et a1. */
    private static void arc(GuiGraphics g, float cx, float cy, float r1, float r2, double a0, double a1, int color) {
        Matrix4f m = g.pose().last().pose();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        int segs = Math.max(2, (int) Math.ceil((a1 - a0) / 0.06));
        double d = (a1 - a0) / segs;
        for (int i = 0; i < segs; i++) {
            double t0 = a0 + i * d, t1 = t0 + d;
            float c0 = (float) Math.cos(t0), s0 = (float) Math.sin(t0);
            float c1 = (float) Math.cos(t1), s1 = (float) Math.sin(t1);
            float ix0 = cx + c0 * r1, iy0 = cy + s0 * r1;
            float ix1 = cx + c1 * r1, iy1 = cy + s1 * r1;
            float ox0 = cx + c0 * r2, oy0 = cy + s0 * r2;
            float ox1 = cx + c1 * r2, oy1 = cy + s1 * r2;

            b.addVertex(m, ix0, iy0, 0).setColor(color);
            b.addVertex(m, ox0, oy0, 0).setColor(color);
            b.addVertex(m, ox1, oy1, 0).setColor(color);

            b.addVertex(m, ix0, iy0, 0).setColor(color);
            b.addVertex(m, ox1, oy1, 0).setColor(color);
            b.addVertex(m, ix1, iy1, 0).setColor(color);
        }
        MeshData mesh = b.build();
        if (mesh != null) BufferUploader.drawWithShader(mesh);

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(null);
    }
}
