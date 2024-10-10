package main.java.br.com.app;

import main.java.br.com.model.Matrix3;
import main.java.br.com.model.Triangle;
import main.java.br.com.model.Vertex;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;
import java.awt.image.BufferedImage;

public class Render {

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        Container pane = frame.getContentPane();
        pane.setLayout(new BorderLayout());

        // slider to control horizontal rotation
        JSlider headingSlider = new JSlider(-180, 180, 0);
        pane.add(headingSlider, BorderLayout.SOUTH);

        // slider to control vertical rotation
        JSlider pitchSlider = new JSlider(SwingConstants.VERTICAL, -90, 90, 0);
        pane.add(pitchSlider, BorderLayout.EAST);

        // panel to display render results
        JPanel renderPanel = new JPanel() {
            public void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(new Color(0x000814)); // Set color background
                g2.fillRect(0, 0, getWidth(), getHeight());

                //Create triangles
                List<Triangle> tris = new ArrayList<>();
                tris.add(new Triangle(new Vertex(100, 100, 100),
                        new Vertex(-100, -100, 100),
                        new Vertex(-100, 100, -100),
                        Color.GRAY));
                tris.add(new Triangle(new Vertex(100, 100, 100),
                        new Vertex(-100, -100, 100),
                        new Vertex(100, -100, -100),
                        Color.GRAY));
                tris.add(new Triangle(new Vertex(-100, 100, -100),
                        new Vertex(100, -100, -100),
                        new Vertex(100, 100, 100),
                        Color.GRAY));
                tris.add(new Triangle(new Vertex(-100, 100, -100),
                        new Vertex(100, -100, -100),
                        new Vertex(-100, -100, 100),
                        Color.GRAY));

                // Apply 'inflate' to the triangles
                for (int i = 0; i < 4; i++) {
                    tris = inflate(tris);
                }


                // Apply rotation XZ matrix to each vertex, heading = headingSlider convert to radians
                double heading = Math.toRadians(headingSlider.getValue());
                Matrix3 rotationXZMatriz = new Matrix3(new double[] {
                        Math.cos(heading), 0, -Math.sin(heading),
                        0, 1, 0,
                        Math.sin(heading), 0, Math.cos(heading)
                });

                // Apply rotation YZ matrix to each vertex, pitch = pitchSilder convert to radians
                double pitch = Math.toRadians(pitchSlider.getValue());
                Matrix3 rotationYZMatrix = new Matrix3(new double[] {
                        1, 0, 0,
                        0, Math.cos(pitch), Math.sin(pitch),
                        0, -Math.sin(pitch), Math.cos(pitch)
                });

                // Multiply rotation XZ and YZ
                Matrix3 rotation = rotationXZMatriz.multiply(rotationYZMatrix);

                BufferedImage img =
                        new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);

                double[] zBuffer = new double[img.getWidth() * img.getHeight()];

                //Initialize array with extremely far away depths
                for(int q = 0; q < zBuffer.length; q++) {
                    zBuffer[q] = Double.NEGATIVE_INFINITY;
                }

                for (Triangle t : tris) {
                    Vertex v1 = rotation.transform(t.getV1());
                    Vertex v2 = rotation.transform(t.getV2());
                    Vertex v3 = rotation.transform(t.getV3());

                    // since we are not using Graphics2D anymore,
                    // we have to do translation manually
                    v1.setX(v1.getX() + getWidth() / 2);
                    v2.setX(v2.getX() + getWidth() / 2);
                    v3.setX(v3.getX() + getWidth() / 2);

                    v1.setY(v1.getY() + getWidth() / 2);
                    v2.setY(v2.getY() + getWidth() / 2);
                    v3.setY(v3.getY() + getWidth() / 2);


                    /*System.out.println("V1X: " + v1.getX() + ",V1Y: " + v1.getY() );
                    System.out.println("V2X: " + v2.getX() + ",V2Y: " + v2.getY() );
                    System.out.println("V3X: " + v3.getX() + ",V3Y: " + v3.getY() );*/

                    // this part is similar to a triangle BAC,
                    //this is AB vertex
                    Vertex ab = new Vertex(v2.getX() - v1.getX(), v2.getY() - v1.getY(), v2.getZ() - v1.getZ());
                    //this is AC vertex
                    Vertex ac = new Vertex(v3.getX() - v1.getX(), v3.getY() - v1.getY(), v3.getZ() - v1.getZ());
                    //this is Vertex normal, perpendicular to abc
                    Vertex norm = new Vertex(
                            ab.getY() * ac.getZ() - ab.getZ() * ac.getY(),
                            ab.getZ() * ac.getX() - ab.getX() * ac.getZ(),
                            ab.getX() * ac.getY() - ab.getY() * ac.getX()
                    );

                    double normalLength =
                            Math.sqrt(norm.getX() * norm.getX() + norm.getY() * norm.getY() + norm.getZ() * norm.getZ());

                    norm.setX(norm.getX() / normalLength);
                    norm.setY(norm.getY() / normalLength);
                    norm.setZ(norm.getZ() / normalLength);


                    // this is the angle between the BAC triangle
                    double angleCos = Math.abs(norm.getZ());

                    // compute rectangular bounds for

                    int minX = (int) Math.max(0, Math.ceil(Math.min(v1.getX(), Math.min(v2.getX(), v3.getX()))));
                    int maxX = (int) Math.min(img.getWidth() - 1,
                            Math.floor(Math.max(v1.getX(), Math.max(v2.getX(), v3.getX()))));

                    int minY = (int) Math.max(0, Math.ceil(Math.min(v1.getY(), Math.min(v2.getY(), v3.getY()))));
                    int maxY = (int) Math.min(img.getHeight() - 1,
                            Math.floor(Math.max(v1.getY(), Math.max(v2.getY(), v3.getY()))));

                    double triangleArea =
                            (v1.getY() - v3.getY()) * (v2.getX() - v3.getX()) + (v2.getY() - v3.getY()) * (v3.getX() - v1.getX());

                    for (int y = minY; y <= maxY; y++) {
                        for (int x = minX; x <= maxX; x++) {
                            double b1 =
                                    ((y - v3.getY()) * (v2.getX() - v3.getX()) + (v2.getY() - v3.getY()) * (v3.getX() - x)) / triangleArea;
                            double b2 =
                                    ((y - v1.getY()) * (v3.getX() - v1.getX()) + (v3.getY() - v1.getY()) * (v1.getX() - x)) / triangleArea;
                            double b3 =
                                    ((y - v2.getY()) * (v1.getX() - v2.getX()) + (v1.getY() - v2.getY()) * (v2.getX() - x)) / triangleArea;

                            if (b1 >= 0 && b1 <= 1 && b2 >= 0 && b2 <= 1 && b3 >= 0 && b3 <= 1) {
                                double depth = b1 * v1.getZ() + b2 * v2.getZ() + b3 * v3.getZ();
                                int zIndex = y * img.getWidth() + x;
                                if(zBuffer[zIndex] < depth){
                                    // color
                                    img.setRGB(x,y,getShade(t.getColor(),angleCos).getRGB());
                                    // update z-buffer
                                    zBuffer[zIndex] = depth;
                                }

                            }


                        }
                    }

                }

                g2.drawImage(img, 0, 0, null);
            }
        };
        pane.add(renderPanel, BorderLayout.CENTER);

        // Render again the render panel when headingSlider or pitchSlider change
        headingSlider.addChangeListener(e -> renderPanel.repaint());
        pitchSlider.addChangeListener(e -> renderPanel.repaint());

        frame.setSize(800, 800);
        frame.setVisible(true);
    }

    // Create the color
    public static Color getShade(Color color, double shade) {
        double redLinear = Math.pow(color.getRed(), 2.4) * shade;
        double greenLinear = Math.pow(color.getGreen(), 2.4) * shade;
        double blueLinear = Math.pow(color.getBlue(), 2.4) * shade;

        int red = (int) Math.pow(redLinear, 1/2.4);
        int green = (int) Math.pow(greenLinear, 1/2.4);
        int blue = (int) Math.pow(blueLinear, 1/2.4);

        return new Color(red, green, blue);
    }


    /*Here's one bonus for you - we can quickly create a sphere approximation from this tetrahedron.
    It can be done by repeatedly subdividing each triangle into four smaller ones and "inflating"*/

    public static List<Triangle> inflate(List<Triangle> tris) {
        List<Triangle> result = new ArrayList<>();
        for (Triangle t : tris) {
            Vertex m1 = new Vertex((t.getV1().getX() + t.getV2().getX())/2, (t.getV1().getY() + t.getV2().getY())/2, (t.getV1().getZ() + t.getV2().getZ())/2);
            Vertex m2 = new Vertex((t.getV2().getX() + t.getV3().getX())/2, (t.getV2().getY() + t.getV3().getY())/2, (t.getV2().getZ() + t.getV3().getZ())/2);
            Vertex m3 = new Vertex((t.getV1().getX() + t.getV3().getX())/2, (t.getV1().getY() + t.getV3().getY())/2, (t.getV1().getZ() + t.getV3().getZ())/2);
            result.add(new Triangle(t.getV1(), m1, m3, t.getColor()));
            result.add(new Triangle(t.getV2(), m1, m2, t.getColor()));
            result.add(new Triangle(t.getV3(), m2, m3, t.getColor()));
            result.add(new Triangle(m1, m2, m3, t.getColor()));
        }
        for (Triangle t : result) {
            for (Vertex v : new Vertex[] { t.getV1(), t.getV2(), t.getV3() }) {
                double l = Math.sqrt(v.getX() * v.getX() + v.getY() * v.getY() + v.getZ() * v.getZ()) / Math.sqrt(30000);
                v.setX(v.getX() / l);
                v.setY(v.getY() / l);
                v.setZ(v.getZ() / l);
            }
        }
        return result;
    }


}

