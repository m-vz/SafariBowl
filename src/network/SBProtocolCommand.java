package network;


import java.util.ArrayList;

/**
 * Enumeration of protocol commands and modules.
 * Created by milan on 23.3.15.
 */
public enum SBProtocolCommand {
    // module CHT:
    /**
     * <b>Chatnachrichten senden.</b><br>
     * Parameter:
     * <table summary="Die Parameter, die erwartet werden.">
     *     <tr>
     *         <th>Parameter</th>
     *         <th>Beschreibung</th>
     *     </tr>
     *     <tr>
     *         <td>
     *             <code>[$recipient&nbsp;$message]</code>
     *         </td>
     *         <td>
     *             Die erste Form von <b>SENDM</b> wird von Clients gesendet und hat als ersten Parameter den Namen des Empfängers.
     *         </td>
     *     </tr>
     *     <tr>
     *         <td>
     *             <code>[$sender&nbsp;$message]</code>
     *         </td>
     *         <td>
     *             Die zweite Form von <b>SENDM</b> wird von Servern gesendet und hat als ersten Parameter den Namen des Senders.
     *         </td>
     *     </tr>
     * </table>
     */
    SENDM(SBProtocolModule.CHT),

    /**
     * <table summary="Die Parameter, die erwartet werden.">
     *     <tr>
     *         <th>Parameter</th>
     *         <th>Beschreibung</th>
     *     </tr>
     *     <tr>
     *         <td>
     *             <code>[$sender&nbsp;$message]</code>
     *         </td>
     *         <td>
     *             <code>$message</code> an alle Benutzer senden.
     *         </td>
     *     </tr>
     * </table>
     */
    BDCST(SBProtocolModule.CHT),


    // module AUT:
    /**
     * <table summary="Die Parameter, die erwartet werden.">
     *     <tr>
     *         <th>Parameter</th>
     *         <th>Beschreibung</th>
     *     </tr>
     *     <tr>
     *         <td>
     *             <code>[]</code>
     *         </td>
     *         <td>
     *             Ausloggen.
     *         </td>
     *     </tr>
     * </table>
     */
    LOGUT(SBProtocolModule.AUT),

    /**
     * <table summary="Die Parameter, die erwartet werden.">
     *     <tr>
     *         <th>Parameter</th>
     *         <th>Beschreibung</th>
     *     </tr>
     *     <tr>
     *         <td>
     *             <code>[$user&nbsp;$password_hash]</code>
     *         </td>
     *         <td>
     *             <code>$user</code> mit <code>$password_hash</code> einloggen.
     *         </td>
     *     </tr>
     * </table>
     */
    LOGIN(SBProtocolModule.AUT),

    /**
     * <table summary="Die Parameter, die erwartet werden.">
     *     <tr>
     *         <th>Parameter</th>
     *         <th>Beschreibung</th>
     *     </tr>
     *     <tr>
     *         <td>
     *             <code>[$user]</code>
     *         </td>
     *         <td>
     *             Existiert bereits ein Benutzer mit diesem Namen?
     *         </td>
     *     </tr>
     * </table>
     */
    EXIST(SBProtocolModule.AUT),

    /**
     * <table summary="Die Parameter, die erwartet werden.">
     *     <tr>
     *         <th>Parameter</th>
     *         <th>Beschreibung</th>
     *     </tr>
     *     <tr>
     *         <td>
     *             <code>[]</code>
     *         </td>
     *         <td>
     *             Liste mit allen angemeldeten Benutzern anfordern.
     *         </td>
     *     </tr>
     * </table>
     */
    LSUSR(SBProtocolModule.AUT),

    /**
     * <table summary="Die Parameter, die erwartet werden.">
     *     <tr>
     *         <th>Parameter</th>
     *         <th>Beschreibung</th>
     *     </tr>
     *     <tr>
     *         <td>
     *             <code>[["name"], ["name", "punkte_spieler", "punkte_gegner"], ["name", ""], ...]</code>
     *         </td>
     *         <td>
     *             Server sendet Liste mit allen angemeldeten Spielern. Erster Parameterarray für Spieler in Lobby, zweiter für Spieler in Spiel und dritter für Spieler, die auf Spiel warten (mit leerem zweitem Parameter).
     *         </td>
     *     </tr>
     * </table>
     */
    UPUSR(SBProtocolModule.AUT),

    /**
     * <table summary="Die Parameter, die erwartet werden.">
     *     <tr>
     *         <th>Parameter</th>
     *         <th>Beschreibung</th>
     *     </tr>
     *     <tr>
     *         <td>
     *             <code>[]</code>
     *         </td>
     *         <td>
     *             Liste mit allen Spielen anfordern.
     *         </td>
     *     </tr>
     * </table>
     */
    LSGAM(SBProtocolModule.AUT),

    /**
     * <table summary="Die Parameter, die erwartet werden.">
     *     <tr>
     *         <th>Parameter</th>
     *         <th>Beschreibung</th>
     *     </tr>
     *     <tr>
     *         <td>
     *             <code>[["spieler1", "spieler2", "punkte1", "punkte2"], ["spieler_auf_spiel_wartend"], ...]</code>
     *         </td>
     *         <td>
     *             Server sendet Liste mit allen laufenden Spielen.
     *         </td>
     *     </tr>
     * </table>
     */
    UPGAM(SBProtocolModule.AUT),

    /**
     * <table summary="Die Parameter, die erwartet werden.">
     *     <tr>
     *         <th>Parameter</th>
     *         <th>Beschreibung</th>
     *     </tr>
     *     <tr>
     *         <td>
     *             <code>[["spieler1", "wins", "losses", "ratio", "scored", "received", "ratio", "casualties"], ...]</code>
     *         </td>
     *         <td>
     *             Server sendet Liste mit den Highscores.
     *         </td>
     *     </tr>
     * </table>
     */
    SCORE(SBProtocolModule.AUT),
    
    /**
     * <table summary="Die Parameter, die erwartet werden.">
     *     <tr>
     *         <th>Parameter</th>
     *         <th>Beschreibung</th>
     *     </tr>
     *     <tr>
     *         <td>
     *             <code>[$user&nbsp;$new_user]</code>
     *         </td>
     *         <td>
     *             Name von <code>$user</code> durch <code>$new_user</code> ersetzen. Kann nur bei eigenem <code>$user</code> (d.h. UID stimmt mit UID von <code>$user</code> überein) oder von user mit OP-Rechten ausgeführt werden.
     *         </td>
     *     </tr>
     * </table>
     */
    CHNGE(SBProtocolModule.AUT),

    /**
     * <table summary="Die Parameter, die erwartet werden.">
     *     <tr>
     *         <th>Parameter</th>
     *         <th>Beschreibung</th>
     *     </tr>
     *     <tr>
     *         <td>
     *             <code>[$user]</code>
     *         </td>
     *         <td>
     *             <code>$user</code> löschen (erfordert OP-Rechte).
     *         </td>
     *     </tr>
     * </table>
     */
    RMUSR(SBProtocolModule.AUT),

    /**
     * <table summary="Die Parameter, die erwartet werden.">
     *     <tr>
     *         <th>Parameter</th>
     *         <th>Beschreibung</th>
     *     </tr>
     *     <tr>
     *         <td>
     *             <code>[$user]</code>
     *         </td>
     *         <td>
     *             <code>$user</code> OP-Rechte erteilen.
     *         </td>
     *     </tr>
     * </table>
     */
    OPUSR(SBProtocolModule.AUT),


    // module GAM:
    /**
     * <table summary="Die Parameter, die erwartet werden.">
     *     <tr>
     *         <th>Parameter</th>
     *         <th>Beschreibung</th>
     *     </tr>
     *     <tr>
     *         <td>
     *             <code>[$action&nbsp;$params[]]</code>
     *         </td>
     *         <td>
     *             <code>$action</code> mit <code>$params[]</code> im aktuellen Spiel ausführen.
     *         </td>
     *     </tr>
     * </table>
     */
    ACTIO(SBProtocolModule.GAM),

    /**
     * <b>Spiel starten.</b><br>
     * Parameter:
     * <table summary="Die Parameter, die erwartet werden.">
     *     <tr>
     *         <th>Parameter</th>
     *         <th>Beschreibung</th>
     *     </tr>
     *     <tr>
     *         <td>
     *             <code>[$opponent_name]</code>
     *         </td>
     *         <td>
     *             Bestätigung/Anfrage vom Server zum Starten eines Spiels gegen Gegner mit Name <code>$opponent_nam</code>e. (Wird von Server gesendet, entweder bei Einladung durch anderen Benutzer oder bei gefundenem zufälligen Match.)
     *         </td>
     *     </tr>
     *     <tr>
     *         <td>
     *             <code>[$invited_user_name]</code>
     *         </td>
     *         <td>
     *             Gegen Benutzer mit Name <code>$invited_user_name</code> ein neues Match starten. Von Client gesendet.
     *         </td>
     *     </tr>
     *     <tr>
     *         <td>
     *             <code>[]</code>
     *         </td>
     *         <td>
     *             Auf zufälligen Gegner für ein neues Match warten. Von Client gesendet.
     *         </td>
     *     </tr>
     * </table>
     */
    START(SBProtocolModule.GAM),

    /**
     * <table summary="Die Parameter, die erwartet werden.">
     *     <tr>
     *         <th>Parameter</th>
     *         <th>Beschreibung</th>
     *     </tr>
     *     <tr>
     *         <td>
     *             <code>[]</code>
     *         </td>
     *         <td>
     *             Den aktuellen Match aufgeben.
     *         </td>
     *     </tr>
     * </table>
     */
    SRNDR(SBProtocolModule.GAM),

    /**
     * <table summary="Die Parameter, die erwartet werden.">
     *     <tr>
     *         <th>Parameter</th>
     *         <th>Beschreibung</th>
     *     </tr>
     *     <tr>
     *         <td>
     *             <code>[$event&nbsp;$params[]]</code>
     *         </td>
     *         <td>
     *             <code>$event</code> mit <code>$params[]</code> an Empfänger senden (z.B. "<code>$params[:user]</code> gewinnt Match").
     *         </td>
     *     </tr>
     * </table>
     */
    EVENT(SBProtocolModule.GAM),


    // other modules:
    /**
     * <table summary="Die Parameter, die erwartet werden.">
     *     <tr>
     *         <th>Parameter</th>
     *         <th>Beschreibung</th>
     *     </tr>
     *     <tr>
     *         <td>
     *             <code>[]</code>
     *         </td>
     *         <td>
     *             Ping an Gegenüber, um Verbindung zu prüfen. (Von Server gesendet)
     *         </td>
     *     </tr>
     * </table>
     */
    PIING(SBProtocolModule.PNG),

    /**
     * <table summary="Die Parameter, die erwartet werden.">
     *     <tr>
     *         <th>Parameter</th>
     *         <th>Beschreibung</th>
     *     </tr>
     *     <tr>
     *         <td>
     *             <code>[]</code>
     *         </td>
     *         <td>
     *             Pong an Gegenüber, um bestehende Verbindung zu bestätigen. (Von Client gesendet)
     *         </td>
     *     </tr>
     * </table>
     */
    POONG(SBProtocolModule.PNG),

    /**
     * <table summary="Die Parameter, die erwartet werden.">
     *     <tr>
     *         <th>Parameter</th>
     *         <th>Beschreibung</th>
     *     </tr>
     *     <tr>
     *         <td>
     *             <code>[$MID&nbsp;$reply]</code>
     *         </td>
     *         <td>
     *             Positive Antwort auf empfangene Nachricht <code>$MID</code> mit <code>$repl</code>y.
     *         </td>
     *     </tr>
     * </table>
     */
    WORKD(SBProtocolModule.SUC),

    /**
     * <table summary="Die Parameter, die erwartet werden.">
     *     <tr>
     *         <th>Parameter</th>
     *         <th>Beschreibung</th>
     *     </tr>
     *     <tr>
     *         <td>
     *             <code>[$MID&nbsp;$reply]</code>
     *         </td>
     *         <td>
     *             Negative Antwort auf empfangene Nachricht <code>$MID</code> mit <code>$reply</code>.
     *         </td>
     *     </tr>
     * </table>
     */
    FAILD(SBProtocolModule.FAI);

    private SBProtocolModule module;

    /**
     * Assign the module of this command on creation.
     * @param module The module of this command.
     */
    SBProtocolCommand(SBProtocolModule module) {
        this.module = module;
    }

    public SBProtocolCommand[] getAllCommandsFromModule(SBProtocolModule module) {
        ArrayList<SBProtocolCommand> commandsFound = new ArrayList<SBProtocolCommand>();
        for(SBProtocolCommand command: values())
            if(command.getModule() == module)
                commandsFound.add(command);
        return commandsFound.toArray(new SBProtocolCommand[commandsFound.size()]);
    }

    /**
     * Get the corresponding module for this command.
     * @return The corresponding module.
     */
    public SBProtocolModule getModule() {
        return module;
    }

    /**
     * Get a string representation of a command and a corresponding moodule.
     * Example: SBProtocolCommand.LOGIN.toString() - "AUT LOGIN"
     * @return The string representation of the command and a corresponding moodule.
     */
    public String toMessageString() {
        return module.toString()+" "+toString();
    }

    /**
     * The protocol modules
     */
    public enum SBProtocolModule {
        CHT, AUT, GAM, PNG, SUC, FAI
    }
}
