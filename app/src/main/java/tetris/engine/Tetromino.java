package tetris.engine;

import java.util.ArrayList;
import java.util.List;

import tetris.utils.Point;
import tetris.engine.type.Rotation;
import tetris.engine.type.RotationState;
import tetris.engine.type.TetrominoType;


/**
 * Polyomino that consists of 4 Minoes 
 */
public class Tetromino extends Piece {
    final int order = 4;
    public Point centre;
    // public List<Mino> children;
    public TetrominoType type; 
    public RotationState rotationState; // direction after n CW rotation, 0: origin, 1: CW-ed, 2: r-180-ed, 3: CCW-ed 
//    Mino[] children;

    public Tetromino(Point centre, TetrominoType type) {
        this.type = type;
        this.centre = centre;
        children = new ArrayList<>();
        this.rotationState = RotationState.ZERO;

        if (type == TetrominoType.I) {
            centre.setX(centre.getX() + 0.5);
            centre.setY(centre.getY() + 0.5);

        } else if (type == TetrominoType.O) {
            centre.setX(centre.getX() + 0.5);
            centre.setY(centre.getY() - 0.5);
        }

        double x = centre.getX();
        double y = centre.getY();


        if (type == TetrominoType.T) {
           children.add(new Mino((int)x, (int)y-1));
           children.add(new Mino((int)x-1, (int)y));
           children.add(new Mino((int)x, (int)y));
           children.add(new Mino((int)x+1, (int)y));
        } else if (type == TetrominoType.I) {
            children.add(new Mino((int)(x-1.5), (int)(y-0.5)));
            children.add(new Mino((int)(x-0.5), (int)(y-0.5)));
            children.add(new Mino((int)(x+0.5), (int)(y-0.5)));
            children.add(new Mino((int)(x+1.5), (int)(y-0.5)));
        } else if (type == TetrominoType.O) {
            children.add(new Mino((int)(x-0.5), (int)(y-0.5)));
            children.add(new Mino((int)(x+0.5), (int)(y-0.5)));
            children.add(new Mino((int)(x-0.5), (int)(y+0.5)));
            children.add(new Mino((int)(x+0.5), (int)(y+0.5)));
        } else if (type == TetrominoType.L) {
            children.add(new Mino((int)x, (int)y));
            children.add(new Mino((int)(x-1), (int)y));
            children.add(new Mino((int)(x+1), (int)y));
            children.add(new Mino((int)(x+1), (int)(y-1)));
        } else if (type == TetrominoType.J) {
            children.add(new Mino((int)x, (int)y));
            children.add(new Mino((int)(x-1), (int)y));
            children.add(new Mino((int)(x+1), (int)y));
            children.add(new Mino((int)(x-1), (int)(y-1)));
        } else if (type == TetrominoType.S) {
            children.add(new Mino((int)x, (int)y));
            children.add(new Mino((int)(x-1), (int)y));
            children.add(new Mino((int)x, (int)(y-1)));
            children.add(new Mino((int)(x+1), (int)(y-1)));
        } else if (type == TetrominoType.Z) {
            children.add(new Mino((int)x, (int)y));
            children.add(new Mino((int)(x+1), (int)y));
            children.add(new Mino((int)x, (int)(y-1)));
            children.add(new Mino((int)(x-1), (int)(y-1)));
        }
    }

    /**
     * duplicates a Tetromino (deep copy)
     * @param t
     */
    public Tetromino(Tetromino t) {
        this.centre = new Point(t.centre);
        this.children = new ArrayList<Mino>();
        for (Mino m : t.children) {
            this.children.add(new Mino(m));
        }

        this.type = t.type;
        this.rotationState = t.rotationState;
    }

    public void moveLeft() {
        double originalX = this.centre.getX();
        this.centre.setX(originalX - 1);
        super.moveLeft();
    }

    public void moveRight() {
        double originalX = this.centre.getX();
        this.centre.setX(originalX + 1);
        super.moveRight();
    }

    public void moveDown() {
        double originalY = this.centre.getY();
        this.centre.setY(originalY + 1);
        super.moveDown();
    }

    public void moveUp() {
        double originalY = this.centre.getY();
        this.centre.setY(originalY - 1);
        super.moveUp();
    }

    /**
    * rotates the tetromino clockwise / right
    */
    public void rotateCounterClockwise() {
        for (Mino m : children) {
            Point p = m.getPosition();
            double distanceToCentreX = p.getX() - centre.getX();
            double distanceToCentreY = p.getY() - centre.getY();

            double newX = distanceToCentreY + centre.getX();
            double newY = -distanceToCentreX + centre.getY();
            p.setX(newX);
            p.setY(newY);

        }
        //rotationState = (rotationState + 3) % 4;
        rotationState = rotationState.changeState(Rotation.COUNTER_CLOCKWISE);
    }

    /**
    * rotates the tetromino counterclockwise / left
    */
    public void rotateClockwise() {
        for (Mino m : children) {
            Point p = m.getPosition();
            double distanceToCentreX = p.getX() - centre.getX();
            double distanceToCentreY = p.getY() - centre.getY();

            double newX = -distanceToCentreY + centre.getX();
            double newY = distanceToCentreX + centre.getY();
            p.setX(newX);
            p.setY(newY);
        }
        rotationState = rotationState.changeState(Rotation.CLOCKWISE);
    }

    /**
    * rotates the tetromino 180 degree
    */
    public void rotate180() {
        for (Mino m : children) {
            Point p = m.getPosition();
            double distanceToCentreX = p.getX() - centre.getX();
            double distanceToCentreY = p.getY() - centre.getY();

            double newX = -distanceToCentreX + centre.getX();
            double newY = -distanceToCentreY + centre.getY();
            p.setX(newX);
            p.setY(newY);
        }
        rotationState = rotationState.changeState(Rotation.R_180);
    }

    public int getOrder() {
        return this.order;
    }

    public Point getCentre() {
        return centre;
    }

    public boolean isSamePosition(Tetromino other) {
        if (children.size() != other.children.size()) {
            return false;
        }
        for (Mino mino : children) {
            boolean found = false;
            for (Mino otherMino : other.children) {
                if (mino.getPosition().equals(otherMino.getPosition())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    public boolean isSamePositionAndRotation(Tetromino other) {
        return this.isSamePosition(other) && this.rotationState == other.rotationState;
    }

    // debug helpers
    public String toString() {
        String str = "tetromino: "+this.type+" at:";
        for (Mino mino : children) {
            str += mino.getPosition().toString();
        }
        str.concat("\n");
        return str;
    }
    
}
