package rails.game.correct;

import java.util.*;

import rails.game.action.LayBaseToken;
import rails.game.action.LayTile;
import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.*;

import com.google.common.collect.ImmutableList;


public final class MapCorrectionManager extends CorrectionManager {

    public static enum ActionStep {
        SELECT_HEX,SELECT_TILE,SELECT_ORIENTATION,CONFIRM,RELAY_BASETOKENS,FINISHED,CANCELLED;
    }

    private MapCorrectionAction activeTileAction = null;

    private MapCorrectionManager(GameManager parent) {
        super(parent, CorrectionType.CORRECT_MAP);
    }

    public static MapCorrectionManager create(GameManager parent) {
        return new MapCorrectionManager(parent);
    }

    @Override
    public List<CorrectionAction> createCorrections() {
        List<CorrectionAction> actions = super.createCorrections();

        if (isActive()) {
            if (activeTileAction == null) {
                activeTileAction = new MapCorrectionAction(getRoot());
            }
            actions.add(activeTileAction);
            // FIXME: This is a workaround to get the LayTile and LayToken actions created from inside the CorrectionManager
            LayTile tileAction = new LayTile(getRoot(), LayTile.CORRECTION);
            getParent().getPossibleActions().add(tileAction);
            for (PublicCompany company:getRoot().getCompanyManager().getAllPublicCompanies()) {
                if (!company.isClosed() && company.hasLaidHomeBaseTokens() && company.getNumberOfFreeBaseTokens() > 0) {
                    LayBaseToken tokenAction = new LayBaseToken(getRoot(), LayBaseToken.CORRECTION);
                    tokenAction.setCompany(company);
                    getParent().getPossibleActions().add(tokenAction);
                }
            }
        }
        return actions;
    }

    @Override
    public boolean executeCorrection(CorrectionAction action){
        if (action instanceof MapCorrectionAction)
            return execute((MapCorrectionAction) action);
        else // any other action, could be a correctionMode action
            return super.executeCorrection(action);
    }

    private boolean execute(MapCorrectionAction action){

        if (action.getStep() == ActionStep.FINISHED) {
            // already finished, thus on reload
            action.setNextStep(ActionStep.FINISHED);
        } else if (action.getNextStep() == ActionStep.CANCELLED) {
            // cancelled => set to null and return
            activeTileAction = null;
            return true;
        }

        MapHex hex = action.getLocation();

        Tile chosenTile = action.getChosenTile();
        TileManager tmgr = getRoot().getTileManager();
        Tile preprintedTile = tmgr.getTile(hex.getPreprintedTileId());

        // check conditions
        String errMsg = null;
        while (true) {
            // check if chosenTile is still available (not for preprinted)
            // FIXME: Check if this is still correct (Rails 2.0), removed that check as all
            // tiles have external id defined
            if (chosenTile != null // && rails.util.Util.hasValue(chosenTile.toText())
                    && chosenTile != hex.getCurrentTile()
                    && chosenTile.getFreeCount() == 0) {
                errMsg =
                    LocalText.getText("TileNotAvailable",
                            chosenTile.toText());
                // return to step of tile selection
                action.selectHex(hex);
                break;
            }
            // check if chosenTile contains enough slots
            Set<BaseToken> baseTokens = hex.getBaseTokens();
            if (chosenTile != null && baseTokens != null && !baseTokens.isEmpty()) {
                Collection<Station> stations = chosenTile.getStations();
                int nbSlots = 0;
                if (stations != null) {
                    for (Station station:stations) {
                        nbSlots += station.getBaseSlots();
                    }
                }
                if (baseTokens.size() > nbSlots) {
                    errMsg =
                        LocalText.getText("CorrectMapNotEnoughSlots", chosenTile.toText());
                    // return to step of tile selection
                    action.selectHex(hex);
                    break;
                }
                // check if chosenTile requires relays
                // this is not implemented yet, thus error message
                if (chosenTile.getNumStations() >= 2
                        && hex.getCurrentTile().getColourNumber() >= chosenTile.getColourNumber()
                        // B. or the current tile requires relays
                        || hex.getCurrentTile().relayBaseTokensOnUpgrade()) {
                    errMsg =
                        LocalText.getText("CorrectMapRequiresRelays", chosenTile.toText());
                    // return to step of tile selection
                    action.selectHex(hex);
                    break;
                }
            }
            break;
        }

        if (errMsg != null) {
            DisplayBuffer.add(this, LocalText.getText("CorrectMapCannotLayTile",
                    chosenTile.toText(),
                    hex.getId(),
                    errMsg ));
            ;
        }

        ActionStep nextStep;
        // not yet finished, move to next step
        if (action.getStep() != ActionStep.FINISHED)
            nextStep = action.getNextStep();
        else
            nextStep = ActionStep.FINISHED;

        // preparation for the next step
        switch (nextStep) {
        case SELECT_TILE:
            // create list of possible up and downgrades
            // REMARK: This is commented out for Rails 2.0
//            List<Tile> possibleTiles = tmgr.getAllUpgrades(preprintedTile, hex);
//            if (preprintedTile == hex.getCurrentTile())
//                possibleTiles.remove(hex.getCurrentTile()); // remove preprinted tile if still laid
//            action.setTiles(possibleTiles);
            break;
        case SELECT_ORIENTATION:
            // default orientation for preprinted files
            if (preprintedTile == chosenTile) {
                int orientation = hex.getPreprintedTileRotation().getTrackPointNumber();
                action.selectOrientation(orientation);
                action.setNextStep(ActionStep.CONFIRM);
                break;
            } else if (chosenTile.getFixedOrientation() != null) {
                int orientation = chosenTile.getFixedOrientation().getTrackPointNumber();
                action.selectOrientation(orientation);
                action.setNextStep(ActionStep.CONFIRM);
                break;
            } else {
                break;
            }
        case RELAY_BASETOKENS:
            // check if relays are necessary:
            // A. downgrades or equalgrades to a tile with two stations or more
            if (chosenTile.getNumStations() >= 2
                    && hex.getCurrentTile().getColourNumber() >= chosenTile.getColourNumber()
                    // B. or the current tile requires relays
                    || hex.getCurrentTile().relayBaseTokensOnUpgrade()) {
                // define tokens for relays
                ImmutableList.Builder<BaseToken> tokens = ImmutableList.builder();
                for (Stop oldStop:hex.getStops()) {
                    tokens.addAll(oldStop.getBaseTokens());
                }
                action.setTokensToRelay(tokens.build());
                // define possible stations
                action.setPossibleStations(chosenTile.getStations());
                break;
            } else {
                action.selectRelayBaseTokens(null);
                // move to FINISHED
                return execute(action);
            }
        case FINISHED:


            // lays tile
            HexSide orientation = HexSide.get(action.getOrientation());
            hex.upgrade(chosenTile, orientation, new HashMap<String,Integer>());

            String msg = LocalText.getText("CorrectMapLaysTileAt",
                    chosenTile.toText(), hex.getId(), hex.getOrientationName(orientation));
            ReportBuffer.add(this, msg);
            getParent().addToNextPlayerMessages(msg, true);

            // relays tokens
            //            if (action.getTokensToRelay() != null) {
            //                for (BaseToken token:action.getTokensToRelay()) {
            //                    int i = action.getTokensToRelay().indexOf(token);
            //
            //                }
            //            }

            activeTileAction = null;
            break;

        case CANCELLED:
            // should be captured above
            activeTileAction = null;
        }

        if (action.getStep() != ActionStep.FINISHED) {
            action.moveToNextStep();
        }

        return true;
    }
}
