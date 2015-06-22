package client.logic;

import gameLogic.Pitch;
import gameLogic.PitchField;

import java.util.ArrayList;

/**
 * Used to store drawn path elements when the move of a Player is planned.
 */
public class DrawingPath {

    private ArrayList<PitchField> path = new ArrayList<PitchField>();
    private Pitch pitch;

    public DrawingPath(Pitch pitch, PitchField firstPos) {
        this.pitch = pitch;
        path.add(firstPos);
    }

    public boolean addPathElement(PitchField element) {
        if (pitch.isAdjacent(path.get(path.size() - 1), element)) { // new path element is adjacent to last added path element
            path.add(element);
            return true;
        } else return false;
    }

    public void removeLastPathElement() {
        path.remove(path.size() - 1);
    }

    public ArrayList<PitchField> getPath() {
        return path;
    }
}
