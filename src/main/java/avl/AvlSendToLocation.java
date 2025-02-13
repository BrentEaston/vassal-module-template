package avl;

import java.awt.Component;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import VASSAL.build.module.Map;
import VASSAL.build.module.documentation.HelpFile;
import VASSAL.build.module.map.boardPicker.Board;
import VASSAL.command.Command;
import VASSAL.configure.ChooseComponentDialog;
import VASSAL.configure.HotKeyConfigurer;
import VASSAL.configure.IntConfigurer;
import VASSAL.configure.StringConfigurer;
import VASSAL.counters.Decorator;
import VASSAL.counters.EditablePiece;
import VASSAL.counters.GamePiece;
import VASSAL.counters.KeyCommand;
import VASSAL.counters.PieceEditor;
import VASSAL.tools.SequenceEncoder;

/*
 * $Id: AvlSendToLocation.java 977 2006-09-12 13:12:53Z swampwallaby $
 *
 * Copyright (c) 2003 by Rodney Kinney
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License (LGPL) as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, copies are available
 * at http://www.opensource.org.
 */

/**
 * This trait adds a command that sends a piece to a particular location on a particular
 * board of a particular Map.
 */
public class AvlSendToLocation extends Decorator implements EditablePiece {
  public static final String ID = "avlsendto;";
  public static final String BACK_MAP = "backMap";
  public static final String BACK_POINT = "backPoint";
  protected KeyCommand[] command;
  protected String commandName;
  protected String backCommandName;
  protected KeyStroke key;
  protected KeyStroke backKey;
  protected String mapId;
  protected String boardName;
  protected int x;
  protected int y;
  protected KeyCommand sendCommand;
  protected KeyCommand backCommand;

  public AvlSendToLocation() {
    this(ID + ";;;;0;0;;", null);
  }

  public AvlSendToLocation(String type, GamePiece inner) {
    mySetType(type);
    setInner(inner);
  }

  public void mySetType(String type) {
    type = type.substring(ID.length());
    SequenceEncoder.Decoder st = new SequenceEncoder.Decoder(type, ';');
    commandName = st.nextToken();
    key = st.nextKeyStroke(null);
    mapId = st.nextToken();
    boardName = st.nextToken();
    x = st.nextInt(0);
    y = st.nextInt(0);
    backCommandName = st.nextToken("");
    backKey = st.nextKeyStroke(null);
  }

  public String myGetType() {
    SequenceEncoder se = new SequenceEncoder(';');
    se.append(commandName)
        .append(key)
        .append(mapId)
        .append(boardName)
        .append(x + "")
        .append(y + "")
        .append(backCommandName)
        .append(backKey);
    return ID + se.getValue();
  }

  protected KeyCommand[] myGetKeyCommands() {
    if (command == null) {
      sendCommand = new KeyCommand(commandName, key, Decorator.getOutermost(this));
      backCommand = new KeyCommand(backCommandName, backKey, Decorator.getOutermost(this));
      List l = new ArrayList();
      if (commandName.length() > 0 && key != null) {
        l.add(sendCommand);
      }
      if (backCommandName.length() > 0 && backKey != null) {
        l.add(backCommand);
      }
      command = (KeyCommand[]) l.toArray(new KeyCommand[l.size()]);
    }
    for (int i = 0; i < command.length; i++) {
      if (command[i].getName().equals(backCommandName)) {
        command[i].setEnabled(getMap() != null && getProperty(BACK_MAP) != null && getProperty(BACK_POINT) != null);
      }
      else {
        command[i].setEnabled(getMap() != null);
      }
    }
    return command;
  }

  public String myGetState() {
    return "";
  }

  public Command myKeyEvent(KeyStroke stroke) {
    Command c = null;
    myGetKeyCommands();
    if (sendCommand.matches(stroke)) {
      Map m = Map.getMapById(mapId);
      if (m == null) {
        m = getMap();
      }
      if (m != null) {
        int offset = 1;
        try {
          offset = (new Integer((String) getProperty("Offset"))).intValue();
        }
        catch (Exception e) {
          
        }
        int x1 = x + (offset-1) * 50;
        Point dest = new Point(x1, y);
        Board b = m.getBoardByName(boardName);
        if (b != null) {
          dest.translate(b.bounds().x, b.bounds().y);
        }
        setProperty(BACK_MAP, getMap());
        setProperty(BACK_POINT, getPosition());
        c = m.placeOrMerge(Decorator.getOutermost(this), dest);
      }
    }
    else if(backCommand.matches(stroke)) {
      Map backMap = (Map) getProperty(BACK_MAP);
      Point backPoint = (Point) getProperty(BACK_POINT);
      if (backMap != null && backPoint != null) {
         c = backMap.placeOrMerge(Decorator.getOutermost(this), backPoint);
      }
      setProperty(BACK_MAP, null);
      setProperty(BACK_POINT, null);
      
    }
    return c;
  }

  public void mySetState(String newState) {
  }

  public Rectangle boundingBox() {
    return piece.boundingBox();
  }

  public void draw(Graphics g, int x, int y, Component obs, double zoom) {
    piece.draw(g, x, y, obs, zoom);
  }

  public String getName() {
    return piece.getName();
  }

  public Shape getShape() {
    return piece.getShape();
  }

  public PieceEditor getEditor() {
    return new Ed(this);
  }

  public String getDescription() {
    return "AVL Send to Location";
  }

  public HelpFile getHelpFile() {
    File dir = VASSAL.build.module.Documentation.getDocumentationBaseDir();
    dir = new File(dir, "ReferenceManual");
    try {
      return new HelpFile(null, new File(dir, "SendToLocation.htm"));
    }
    catch (MalformedURLException ex) {
      return null;
    }
  }

  public static class Ed implements PieceEditor {
    private StringConfigurer nameInput;
    private StringConfigurer backNameInput;
    private HotKeyConfigurer keyInput;
    private HotKeyConfigurer backKeyInput;
    private JTextField mapIdInput;
    private JTextField boardNameInput;
    private IntConfigurer xInput;
    private IntConfigurer yInput;
    private Map map;
    private JPanel controls;

    public Ed(AvlSendToLocation p) {
      controls = new JPanel();
      controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));

      nameInput = new StringConfigurer(null, "Command name:  ", p.commandName);
      controls.add(nameInput.getControls());

      keyInput = new HotKeyConfigurer(null,"Keyboard Command:  ",p.key);
      controls.add(keyInput.getControls());

      backNameInput = new StringConfigurer(null, "Send Back Command name:  ", p.backCommandName);
      controls.add(backNameInput.getControls());

      backKeyInput = new HotKeyConfigurer(null,"Send Back Keyboard Command:  ",p.backKey);
      controls.add(backKeyInput.getControls());
      
      Box b = Box.createHorizontalBox();
      mapIdInput = new JTextField(12);
      map = Map.getMapById(p.mapId);
      if (map != null) {
        mapIdInput.setText(map.getMapName());
      }
      mapIdInput.setEditable(false);
      b.add(new JLabel("Map:  "));
      b.add(mapIdInput);
      JButton select = new JButton("Select");
      select.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          selectMap();
        }
      });
      b.add(select);
      JButton clear = new JButton("Clear");
      clear.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          clearMap();
        }
      });
      b.add(clear);
      controls.add(b);

      b = Box.createHorizontalBox();
      boardNameInput = new JTextField(12);
      boardNameInput.setText(p.boardName);
      boardNameInput.setEditable(false);
      b.add(new JLabel("Board:  "));
      b.add(boardNameInput);
      select = new JButton("Select");
      select.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          selectBoard();
        }
      });
      clear = new JButton("Clear");
      clear.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          clearBoard();
        }
      });
      b.add(select);
      b.add(clear);
      controls.add(b);

      xInput = new IntConfigurer(null, "X Position:  ", new Integer(p.x));
      controls.add(xInput.getControls());

      yInput = new IntConfigurer(null, "Y Position:  ", new Integer(p.y));
      controls.add(yInput.getControls());
      
    }

    private void clearBoard() {
      boardNameInput.setText("");
    }

    private void clearMap() {
      map = null;
      mapIdInput.setText("");
    }

    private void selectBoard() {
      ChooseComponentDialog d = new ChooseComponentDialog((Frame) SwingUtilities.getAncestorOfClass(Frame.class, controls), Board.class);
      d.setVisible(true);
      if (d.getTarget() != null) {
        Board b = (Board) d.getTarget();
        boardNameInput.setText(b.getName());
      }
    }

    private void selectMap() {
      ChooseComponentDialog d = new ChooseComponentDialog((Frame) SwingUtilities.getAncestorOfClass(Frame.class, controls), Map.class);
      d.setVisible(true);
      if (d.getTarget() != null) {
        map = (Map) d.getTarget();
        mapIdInput.setText(map.getMapName());
      }
    }

    public Component getControls() {
      return controls;
    }

    public String getType() {
      SequenceEncoder se = new SequenceEncoder(';');
      se.append(nameInput.getValueString())
          .append((KeyStroke)keyInput.getValue())
          .append(map == null ? "" : map.getIdentifier())
          .append(boardNameInput.getText())
          .append(xInput.getValueString())
          .append(yInput.getValueString())
          .append(backNameInput.getValueString())
          .append((KeyStroke)backKeyInput.getValue());
      return ID + se.getValue();
    }

    public String getState() {
      return "";
    }
  }
}


