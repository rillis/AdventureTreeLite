package com.game.graphics;

import com.game.*;
import com.game.graphics.screens.*;
import com.game.input.*;
import com.game.state.*;
import com.game.world.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;

public class Renderer {
    public static Thread threadRender;
    public static boolean rendering = true;

    private static Canvas canvas;

    private static Dimension canvasSize;
    public static Dimension gameSize = new Dimension(100,100);
    public static float screenFactor;

    private static long lastFPSCheck = 0;
    private static int totalFrames;
    private static final int targetTime = 1000000000 / 100;

    public static void init(){
        Frame frame = new Frame();

        canvas = new Canvas();
        canvasSize = optimizeScreen();
        screenFactor = (float) canvasSize.height / (float) gameSize.height;
        canvas.setPreferredSize(canvasSize);

        frame.add(canvas);
        frame.setUndecorated(true);
        frame.pack();
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                Game.quit();
            }

            @Override
            public void windowClosed(WindowEvent e){
                Game.quit();
            }
        });

        frame.setVisible(true);
        frame.toFront();
        frame.requestFocus();

        canvas.addMouseListener(new MouseInput());

        Tree.init();
        startRendering();
    }

    private static Dimension optimizeScreen() {
        int sH = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().height;
        return new Dimension(Math.min(sH, 800), Math.min(sH, 800));
    }

    private static void startRendering() {
        threadRender = new Thread(() -> {
            GraphicsConfiguration gc = canvas.getGraphicsConfiguration();
            VolatileImage vi = gc.createCompatibleVolatileImage(gameSize.width, gameSize.height);

            while(rendering){
                long startTime = System.nanoTime();

                totalFrames++;
                if (System.nanoTime() > lastFPSCheck+ 1000000000){
                    lastFPSCheck = System.nanoTime();
                    totalFrames = 0;
                }

                if (vi.validate(gc)== VolatileImage.IMAGE_INCOMPATIBLE){
                    vi = gc.createCompatibleVolatileImage(gameSize.width, gameSize.height);
                }

                Graphics g = vi.getGraphics();
                g.setColor(Color.black);
                g.fillRect(0,0, gameSize.width, gameSize.height); // COMEÇO RENDER

                if(GameState.state == GameState.IN_GAME) {
                    Terrain.update();
                    Terrain.render(g);

                    g.setColor(Color.BLACK);
                    g.setFont(getFont("TeenyTinyPixls"));
                    g.drawString(formatCoins(Shop.coins),43,83);
                }else if(GameState.state == GameState.MENU){
                    new StaticSprite(0,0,false,"menu").render(g);
                }else{
                    Shop.render(g);
                    Rectangle r = Shop.slots[Tree.selectedTree-1];
                    g.setColor(new Color(60, 60, 60));
                    g.drawRect(r.x,r.y,r.width-1,r.height-1);
                }

                g.dispose(); //FINAL RENDER

                g = canvas.getGraphics();
                g.drawImage(vi, 0, 0, canvasSize.width, canvasSize.height, null);
                g.dispose();

                long totalTime = System.nanoTime() - startTime;

                if(totalTime < targetTime){
                    try {
                        Thread.sleep((targetTime-totalTime)/1000000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        threadRender.setName("Thread de renderizacao");
        threadRender.start();
    }

    public static BufferedImage loadImage(String path){
        BufferedImage rawImage = null;
        try {
            rawImage = ImageIO.read(Renderer.class.getResource(path));
        } catch (IOException e) {
            e.printStackTrace();
        }
        BufferedImage finalImage = canvas.getGraphicsConfiguration().createCompatibleImage(rawImage.getWidth(), rawImage.getHeight(), rawImage.getTransparency());
        finalImage.getGraphics().drawImage(rawImage,0,0, rawImage.getWidth(), rawImage.getHeight(), null);
        return finalImage;
    }

    static Font getFont(String path){
        try { return Font.createFont(Font.TRUETYPE_FONT, Renderer.class.getResourceAsStream("/com/game/resources/"+path+".ttf")).deriveFont(5f);} catch (FontFormatException | IOException e) {e.printStackTrace(); }
        return null;
    }

    public static String formatCoins(long coins){
        if (coins<1000){return ""+coins;}
        if (coins<1000000){return ""+Math.floor(((double)coins/1000.0)*10)/10+"k";}
        return ""+Math.floor(((double)coins/1000000.0)*10)/10+"M";
    }
}
