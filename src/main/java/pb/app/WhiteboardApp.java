package pb.app;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import pb.WhiteboardServer;

import pb.managers.ClientManager;
import pb.managers.IOThread;
import pb.managers.PeerManager;
import pb.managers.ServerManager;
import pb.managers.endpoint.Endpoint;

import static pb.WhiteboardServer.*;
import static pb.managers.PeerManager.peerServerManager;
import static pb.managers.PeerManager.peerStopped;
import pb.protocols.session.SessionProtocol;


/**
 * Initial code obtained from:
 * https://www.ssaurel.com/blog/learn-how-to-make-a-swing-painting-and-drawing-application/
 */
public class WhiteboardApp {
	private static Logger log = Logger.getLogger(WhiteboardApp.class.getName());
	
	/**
	 * Emitted to another peer to subscribe to updates for the given board. Argument
	 * must have format "host:port:boardid".
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String listenBoard = "BOARD_LISTEN";

	/**
	 * Emitted to another peer to unsubscribe to updates for the given board.
	 * Argument must have format "host:port:boardid".
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String unlistenBoard = "BOARD_UNLISTEN";

	/**
	 * Emitted to another peer to get the entire board data for a given board.
	 * Argument must have format "host:port:boardid".
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String getBoardData = "GET_BOARD_DATA";

	/**
	 * Emitted to another peer to give the entire board data for a given board.
	 * Argument must have format "host:port:boardid%version%PATHS".
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardData = "BOARD_DATA";

	/**
	 * Emitted to another peer to add a path to a board managed by that peer.
	 * Argument must have format "host:port:boardid%version%PATH". The numeric value
	 * of version must be equal to the version of the board without the PATH added,
	 * i.e. the current version of the board.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardPathUpdate = "BOARD_PATH_UPDATE";

	/**
	 * Emitted to another peer to indicate a new path has been accepted. Argument
	 * must have format "host:port:boardid%version%PATH". The numeric value of
	 * version must be equal to the version of the board without the PATH added,
	 * i.e. the current version of the board.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardPathAccepted = "BOARD_PATH_ACCEPTED";

	/**
	 * Emitted to remote peer the updated board. Argument
	 * must have format "host:port:boardid%version%PATH".
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String updateToRemote = "UPDATE_TO_REMOTE";

	/**
	 * Emitted to another peer to remove the last path on a board managed by that
	 * peer. Argument must have format "host:port:boardid%version%". The numeric
	 * value of version must be equal to the version of the board without the undo
	 * applied, i.e. the current version of the board.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardUndoUpdate = "BOARD_UNDO_UPDATE";

	/**
	 * Emitted to another peer to indicate an undo has been accepted. Argument must
	 * have format "host:port:boardid%version%". The numeric value of version must
	 * be equal to the version of the board without the undo applied, i.e. the
	 * current version of the board.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardUndoAccepted = "BOARD_UNDO_ACCEPTED";

	/**
	 * Emitted to another peer to clear a board managed by that peer. Argument must
	 * have format "host:port:boardid%version%". The numeric value of version must
	 * be equal to the version of the board without the clear applied, i.e. the
	 * current version of the board.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardClearUpdate = "BOARD_CLEAR_UPDATE";

	/**
	 * Emitted to another peer to indicate an clear has been accepted. Argument must
	 * have format "host:port:boardid%version%". The numeric value of version must
	 * be equal to the version of the board without the clear applied, i.e. the
	 * current version of the board.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardClearAccepted = "BOARD_CLEAR_ACCEPTED";

	/**
	 * Emitted to another peer to indicate a board no longer exists and should be
	 * deleted. Argument must have format "host:port:boardid".
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardDeleted = "BOARD_DELETED";

	/**
	 * Emitted to another peer to indicate an error has occurred.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardError = "BOARD_ERROR";
	
	/**
	 * White board map from board name to board object 
	 */
	Map<String,Whiteboard> whiteboards;
	
	/**
	 * The currently selected white board
	 */
	Whiteboard selectedBoard = null;
	
	/**
	 * The peer:port string of the peer. This is synonomous with IP:port, host:port,
	 * etc. where it may appear in comments.
	 */
	String peerport="standalone"; // a default value for the non-distributed version
        
        private static String whiteboardServerHost = null;
        
        private static int whiteboardServerPort;
        
        private PeerManager peerManager;
        
        private Endpoint WBServerEndpoint;
        
        private boolean standalone=true;
        
        // stores existing connections with peers
        // key: peers'id in the form of "peer:port:boardId", value: endpoint
        Map<String,Endpoint> peerEndpoints;
        
        // stores the names of the shared boards that are currently being listened(looked at/selected) by peers
        // key: shared board name, value: a list of endpoints that are listening this board
        Map<String,ArrayList<Endpoint>> boardListenedByPeer;

	// stores connections with peers who booked listen
	// key: peers'id in the form of "peer:port", value: endpoint
//        Map<String, Endpoint> requestListeningPeer;
	
	/*
	 * GUI objects, you probably don't need to modify these things... you don't
	 * need to modify these things... don't modify these things [LOTR reference?].
	 */
	
	JButton clearBtn, blackBtn, redBtn, createBoardBtn, deleteBoardBtn, undoBtn;
	JCheckBox sharedCheckbox ;
	DrawArea drawArea;
	JComboBox<String> boardComboBox;
	boolean modifyingComboBox=false;
	boolean modifyingCheckBox=false;
	
	/**
	 * Initialize the white board app.
	 */
	public WhiteboardApp(int peerPort,String whiteboardServerHost, 
			int whiteboardServerPort) {
				whiteboards=new HashMap<>();
                peerEndpoints = new HashMap<>();
                boardListenedByPeer = new HashMap<>();
                this.whiteboardServerHost = whiteboardServerHost;
                this.whiteboardServerPort = whiteboardServerPort;
                
                this.peerManager = new PeerManager(peerPort); 
                
                peerManager.on(PeerManager.peerStarted, (args) -> {
                        Endpoint endpoint = (Endpoint)args[0];
                        System.out.println("Connection from peer: "+endpoint.getOtherEndpointId());
//                        peerEndpoints.put(endpoint.getOtherEndpointId(),endpoint);
                        endpoint.on(getBoardData,(args2)-> {
							String requestedBoardName = (String) args2[0];
							Whiteboard wb = whiteboards.get(requestedBoardName); // TODO: null
//							System.out.println("white board " + wb + " got from whiteboards using key " + requestedBoardName);
							if (wb != null) {
								endpoint.emit(boardData, wb.toString());
							}
						}).on(unlistenBoard, (args2)->{
							String requestedBoardName = (String)args2[0];
							log.info("OnBoardListen "+ requestedBoardName);
							if (this.boardListenedByPeer.get(requestedBoardName) != null) {
								this.boardListenedByPeer.get(requestedBoardName).remove(endpoint);
							}
                        }).on(listenBoard, (args2)->{
                                String requestedBoardName = (String)args2[0];
                                log.info("OnBoardListen "+ requestedBoardName);
                                //connection exist
                                if (this.boardListenedByPeer.get(requestedBoardName) != null){
                                        this.boardListenedByPeer.get(requestedBoardName).add(endpoint);
                                }else{//first connection
                                        this.boardListenedByPeer.put(requestedBoardName, new ArrayList<Endpoint>());
                                        this.boardListenedByPeer.get(requestedBoardName).add(endpoint);
                                }
						}).on(boardUndoUpdate, (args2)->{
							String boardName = getBoardName((String)args2[0]);
							Whiteboard wb = whiteboards.get(boardName);
							//not yet executed
							if(wb.getVersion() != getBoardVersion((String)args2[0])){
								//success
								if (wb.undo(getBoardVersion(wb.toString()))){
									//save to whiteboards
									whiteboards.put(boardName,wb);
									drawSelectedWhiteboard();
									//send new board to listeners
									ArrayList<Endpoint> endpointList = boardListenedByPeer.get(boardName);
									for(Endpoint endpoint1:endpointList){
										if(!endpoint1.equals(endpoint)){
											endpoint1.emit(boardUndoUpdate,args2);
										}
									}
									endpoint.emit(boardUndoAccepted,args2);
								}
							}
						}).on(boardUndoAccepted, (args2)->{
							log.info("host peer accepted undo request successfully");
						}).on(boardClearUpdate, (args2)->{
							String boardName = getBoardName((String)args2[0]);
							Whiteboard wb = whiteboards.get(boardName);
							//not yet executed
							if(wb.getVersion() != getBoardVersion((String)args2[0])){
								//success
								if (wb.clear(getBoardVersion(wb.toString()))){
									//save to whiteboards
									whiteboards.put(boardName,wb);
									drawSelectedWhiteboard();
									//send new board to listeners
									ArrayList<Endpoint> endpointList = boardListenedByPeer.get(boardName);
									for(Endpoint endpoint1:endpointList){
										if(!endpoint1.equals(endpoint)){
											endpoint1.emit(boardClearUpdate,args2);
										}
									}
									endpoint.emit(boardClearAccepted,args2);
								}
							}
						}).on(boardClearAccepted, (args2)->{
							log.info("host peer accepted clear request successfully");
//						}).on(boardDeleted, (args2)->{
						//remote peer add path to host peer
                        }).on(boardPathUpdate, (args2)->{
                        	onBoardPath((String)args2[0]);
                        	endpoint.emit(boardPathAccepted,args2);
                        	String boardName = getBoardName((String)args2[0]);
                        	Whiteboard wb = whiteboards.get(boardName);
                        	ArrayList<Endpoint> endpointList = boardListenedByPeer.get(boardName);
                        	for(Endpoint endpoint1:endpointList){
                        		if(!endpoint1.equals(endpoint)){
									endpoint1.emit(updateToRemote,args2);
								}
							}
                        });
                }).on(peerStopped,(args)->{
                        Endpoint endpoint = (Endpoint)args[0];
                        System.out.println("Disconnected with peer: "+endpoint.getOtherEndpointId());
//                        endpoint.close();
//                	peerManager.shutdown();
                }).on(PeerManager.peerError,(args)->{
                        
                }).on(PeerManager.peerServerManager, (args)->{
                        ServerManager serverManager = (ServerManager)args[0];                    
                        serverManager.on(IOThread.ioThread, (args2)->{
                            peerport = (String) args2[0];
                            log.info("peer server started on port: " + peerport); 
                        });
                });
                
                
                peerManager.start();
                
                // to allow the initialization finish, so that the window title can update
                try {
                        Thread.sleep(300);
                } catch (InterruptedException ex) {

                }
                //String peer:port standlone
                show(peerport);
                
                try {
                        this.connectWBserver(peerManager, peerport);        
                } catch (UnknownHostException ex) {
                        System.out.println("Cannot find the whiteboard server...");
                        log.severe("unknown whiteboard host error");
                } catch (InterruptedException ex) {
                        System.out.println("Connection to whiteboard server interrupted");
                        log.severe("connection to whiteboard server interrupted");
                }
                
		          
	}
        

        private void onBoardPath(String boardInfo){
                System.out.println("Board path update " + boardInfo);
                String name = getBoardName(boardInfo);
                long version = getBoardVersion(boardInfo);
                String path = getBoardPaths(boardInfo);
                // if the board is selected, update and draw
//                System.out.println("name "+ name);
//                System.out.println("selectedBoardName"+selectedBoard.getName());
                if (name.equals(selectedBoard.getName())){
					selectedBoard.addPath(new WhiteboardPath(path),selectedBoard.getVersion());
					whiteboards.put(selectedBoard.getName(),selectedBoard);
					drawSelectedWhiteboard();
                // if the board is not selected, just update. version check??
                }else{
                        Whiteboard wb = whiteboards.get(name);
                        if (wb != null){
                                if(wb.addPath(new WhiteboardPath(path),wb.getVersion())){
                                	whiteboards.put(wb.getName(),wb);
									drawSelectedWhiteboard();
                                        log.info("Whiteboard updated with new path "+path);
                                }else{
                                        log.severe("Whiteboard version error");
                                };
                        }
                }
        }
	
        // peers establish connection with whiteboard server as soon as it starts
        public void connectWBserver(PeerManager peerManager, String peerport)
			throws UnknownHostException, InterruptedException {
		
		ClientManager clientManager = peerManager.connect(this.whiteboardServerPort, this.whiteboardServerHost);

		clientManager.on(PeerManager.peerStarted, (args) -> {
                        System.out.println("Connected to the whiteboard server...");
                        log.info("connection to whiteboard server established"); 
                        this.standalone = false;
			WBServerEndpoint = (Endpoint) args[0];
                        WBServerEndpoint.on(WhiteboardServer.sharingBoard, (args2)->{
                                String sharingBoardName = (String) args2[0];
                                if (!sharingBoard.equals("")) {
                                        Whiteboard wb = new Whiteboard(sharingBoardName,true);
                                        addBoard(wb,false);
                                        System.out.println("Received board "+ sharingBoardName + " from whiteboard server");
                                        
                                }
                        }).on(WhiteboardServer.unsharingBoard, (args2)->{
                                String unsharingBoardName = (String) args2[0];
                                this.deleteBoard(unsharingBoardName);
                                System.out.println("Deleted board "+ unsharingBoardName);
                        });           
		}).on(peerStopped, (args) -> {
			System.out.println("Disconnected to the whiteboard server...");
                        log.info("disconnect from whiteboard server finishes"); 
                        for (String boardName:peerEndpoints.keySet()){
                                peerEndpoints.get(boardName).close();
                        }
                        peerEndpoints.clear();
		}).on(PeerManager.peerError, (args) -> {
			System.out.println("Lost connection to whiteboard server...");
                        log.severe("connection to whiteboard server error");
                        for (String boardName:peerEndpoints.keySet()){
                                peerEndpoints.get(boardName).close();
                        }
                        peerEndpoints.clear();
		});

		clientManager.start();

		clientManager.join();
                
                
	}
        
        
        /**
	 * Get initial boardData from the peer after selecting the board
         * if no connection has been established, establish the connection
	 */
        //boardname is shared board selected from other peer
        private void getInitialBoardData(PeerManager peerManager, String boardName){
            String [] boardNameInfo = boardName.split(":");
            int peerPort = Integer.valueOf(boardNameInfo[1]);
            String peerHost = boardNameInfo[0];
            String IPandPort = boardNameInfo[0]+":"+boardNameInfo[1];
            try {
                if (peerEndpoints.containsKey(boardName)){
//                        System.out.println("existing connection from peerendpoitns");
//                        System.out.println("IPandPort" + IPandPort);
//                        System.out.println("board to get" + boardName);
//                        System.out.println("whiteboard size "+whiteboards.size());
                        Endpoint endpoint = peerEndpoints.get(boardName);
                        endpoint.emit(getBoardData, boardName);
                        drawSelectedWhiteboard();
                }else{
                        ClientManager clientManager = peerManager.connect(peerPort, peerHost);
                        clientManager.on(PeerManager.peerStarted, (args)->{
                                Endpoint endpoint = (Endpoint)args[0];
                                peerEndpoints.put(boardName, endpoint);
                                System.out.println("Connected to peer "+endpoint.getOtherEndpointId());
                                endpoint.on(boardData,(args2)->{
                                        this.onBoardData(boardName,(String)args2[0]);
                                        selectedBoard = whiteboards.get(boardName);
                                        drawSelectedWhiteboard();
								}).on(boardPathAccepted, (args2)->{
									log.info("board path accepted");
								}).on(updateToRemote,(args3) ->{
									onBoardPath((String)args3[0]);
								}).on(boardUndoUpdate, (args2)->{
									Whiteboard wb = whiteboards.get(boardName);
									//not yet executed
									if(wb.getVersion() != getBoardVersion((String)args2[0])){
										//success
										if (wb.undo(getBoardVersion(wb.toString()))){
											//save to whiteboards
											whiteboards.put(boardName,wb);
											drawSelectedWhiteboard();
										}
									}
								}).on(boardUndoAccepted, (args2)->{
									log.info("remote peer accepted undo request successfully");
								}).on(boardClearUpdate, (args2)->{
									Whiteboard wb = whiteboards.get(boardName);
									//not yet executed
									if(wb.getVersion() != getBoardVersion((String)args2[0])){
										//success
										if (wb.clear(getBoardVersion(wb.toString()))){
											//save to whiteboards
											whiteboards.put(boardName,wb);
											drawSelectedWhiteboard();
										}
									}
								}).on(boardClearAccepted, (args2)->{
									log.info("remote peer accepted clear request successfully");
								}).on(boardDeleted, (args2)->{
									deleteBoard((String)args2[0]);
//									clientManager.shutdown();
								});
                                endpoint.emit(getBoardData, boardName);
                                endpoint.emit(listenBoard, boardName);
                        }).on(peerStopped, (args)->{
                                Endpoint endpoint = (Endpoint)args[0];
                                System.out.println("Disconnected from peer: "+endpoint.getOtherEndpointId());
                        }).on(PeerManager.peerError, (args)->{
                                Endpoint endpoint = (Endpoint)args[0];
                                System.out.println("There was error while communication with peer: "
                                                +endpoint.getOtherEndpointId());
                        });
                        clientManager.start();
                }

            } catch (UnknownHostException ex) {
                    System.out.println("Cannot find the peer host...");
                    log.severe("unknown peer host error");
            } catch (InterruptedException ex) {
                    System.out.println("Connection to peer interrupted");
                    log.severe("connection to peer interrupted");
            }   
        }

        //update whiteboard map
        private void onBoardData(String boardName, String args){
                log.info("Onboard Data: "+ args);
                String boardData = getBoardData(args);
                Whiteboard wb = whiteboards.get(boardName);
                if (wb != null){
                    wb.whiteboardFromString(boardName,boardData);
                }
                whiteboards.put(boardName, wb); 
//                selectedBoard.whiteboardFromString(boardName,boardData); 
//                System.out.println("key: "+boardName+"whiteboard "+whiteboards.get(boardName));
//                System.out.println("selected board "+selectedBoard);
        }
       
        
        
	/******
	 * 
	 * Utility methods to extract fields from argument strings.
	 * 
	 ******/
	
	/**
	 * 
	 * @param data = peer:port:boardid%version%PATHS
	 * @return peer:port:boardid
	 */
	public static String getBoardName(String data) {
		String[] parts=data.split("%",2);
		return parts[0];
	}
	
	/**
	 * 
	 * @param data = peer:port:boardid%version%PATHS
	 * @return boardid%version%PATHS
	 */
	public static String getBoardIdAndData(String data) {
		String[] parts=data.split(":");
		return parts[2];
	}
	
	/**
	 * 
	 * @param data = peer:port:boardid%version%PATHS
	 * @return version%PATHS
	 */
	public static String getBoardData(String data) {
		String[] parts=data.split("%",2);
		return parts[1];
	}
	
	/**
	 * 
	 * @param data = peer:port:boardid%version%PATHS
	 * @return version
	 */
	public static long getBoardVersion(String data) {
		String[] parts=data.split("%",3);
		return Long.parseLong(parts[1]);
	}
	
	/**
	 * 
	 * @param data = peer:port:boardid%version%PATHS
	 * @return PATHS
	 */
	public static String getBoardPaths(String data) {
		String[] parts=data.split("%",3);
		return parts[2];
	}
	
	/**
	 * 
	 * @param data = peer:port:boardid%version%PATHS
	 * @return peer
	 */
	public static String getIP(String data) {
		String[] parts=data.split(":");
		return parts[0];
	}
	
	/**
	 * 
	 * @param data = peer:port:boardid%version%PATHS
	 * @return port
	 */
	public static int getPort(String data) {
		String[] parts=data.split(":");
		return Integer.parseInt(parts[1]);
	}
	
	/******
	 * 
	 * Methods called from events.
	 * 
	 ******/
	
	// From whiteboard server
	
	
	// From whiteboard peer
	
	
	
	/******
	 * 
	 * Methods to manipulate data locally. Distributed systems related code has been
	 * cut from these methods.
	 * 
	 ******/
	
	/**
	 * Wait for the peer manager to finish all threads.
	 */
	public void waitToFinish() {
                this.peerManager.joinWithClientManagers();
	}
	
	/**
	 * Add a board to the list that the user can select from. If select is
	 * true then also select this board.
	 * @param whiteboard
	 * @param select
	 */
	public void addBoard(Whiteboard whiteboard,boolean select) {
		synchronized(whiteboards) {
//                        System.out.println("added board name "+ whiteboard.getName());
			whiteboards.put(whiteboard.getName(), whiteboard);
		}
		updateComboBox(select?whiteboard.getName():null);
	}
	
	/**
	 * Delete a board from the list.
	 * @param boardname must have the form peer:port:boardid
	 */
	public void deleteBoard(String boardname) {
		synchronized(whiteboards) {
			Whiteboard whiteboard = whiteboards.get(boardname);
			if(whiteboard!=null) {
				//is local board
				if (!whiteboard.isRemote()){
					ArrayList<Endpoint> endpointList = boardListenedByPeer.get(boardname);
					if (endpointList != null){
						for(int i = 0; i < endpointList.size(); i++){
							Endpoint endpoint = endpointList.get(i);
							endpoint.emit(boardDeleted,boardname);
						}
					}
				}
				whiteboards.remove(boardname);
				if(whiteboard.isShared()){
					WBServerEndpoint.emit(unshareBoard,boardname);
				}
			}
		}
		updateComboBox(null);
	}
	
	/**
	 * Create a new local board with name peer:port:boardid.
	 * The boardid includes the time stamp that the board was created at.
	 */
	public void createBoard() {
		String name = peerport+":board"+Instant.now().toEpochMilli();
		Whiteboard whiteboard = new Whiteboard(name,false);
		addBoard(whiteboard,true);
	}
	
	/**
	 * Add a path to the selected board. The path has already
	 * been drawn on the draw area; so if it can't be accepted then
	 * the board needs to be redrawn without it.
	 * @param currentPath
	 */
	public void pathCreatedLocally(WhiteboardPath currentPath) {
		if(selectedBoard!=null) {
            //fail to draw the new path
			if(!selectedBoard.addPath(currentPath,selectedBoard.getVersion())) {
				// some other peer modified the board in between
				drawSelectedWhiteboard(); // just redraw the screen without the path
			} else {
				// when we draw on the remote board, always emit the update to the host peer
				if (selectedBoard.isRemote()){
						String host = getIP(selectedBoard.getName());
						String port = Integer.toString(getPort(selectedBoard.getName())) ;
						String IPandPort = host+":"+port;
						Endpoint endpoint = peerEndpoints.get(selectedBoard.getName());
//						System.out.println(selectedBoard.getName()+"%"+selectedBoard.getVersion()+"%"+currentPath);
						endpoint.emit(boardPathUpdate, selectedBoard.getName()+"%"+selectedBoard.getVersion()+"%"+currentPath);
				}
				else{
					ArrayList<Endpoint> endpointList = boardListenedByPeer.get(selectedBoard.getName());
					if(endpointList != null){
						for(int i = 0; i < endpointList.size(); i++){
							Endpoint endpoint = endpointList.get(i);
							System.out.println("localpathupdate :"+selectedBoard.getName()+"%"+selectedBoard.getVersion()+"%"+currentPath);
							endpoint.emit(updateToRemote, selectedBoard.getName()+"%"+selectedBoard.getVersion()+"%"+currentPath);
						}
					}
				}
			}
		} else {
			log.severe("path created without a selected board: "+currentPath);
		}
	}
	
	/**
	 * Clear the selected whiteboard.
	 */
	public void clearedLocally() {
		if(selectedBoard!=null) {
			if(!selectedBoard.clear(selectedBoard.getVersion())) {
				// some other peer modified the board in between
				drawSelectedWhiteboard();
			} else {
				// was accepted locally, so do remote stuff if needed
				drawSelectedWhiteboard();
				if (selectedBoard.isRemote()){
					String host = getIP(selectedBoard.getName());
					String port = Integer.toString(getPort(selectedBoard.getName())) ;
					String IPandPort = host+":"+port;
					Endpoint endpoint = peerEndpoints.get(selectedBoard.getName());
					endpoint.emit(boardClearUpdate,selectedBoard.toString());
				}
				else{
					ArrayList<Endpoint> endpointList = boardListenedByPeer.get(selectedBoard.getName());
					for(int i = 0; i < endpointList.size(); i++){
						Endpoint endpoint = endpointList.get(i);
						endpoint.emit(boardClearUpdate,selectedBoard.toString());
					}
				}
			}
		} else {
			log.severe("cleared without a selected board");
		}
	}
	
	/**
	 * Undo the last path of the selected whiteboard.
	 */
	public void undoLocally() {
		if(selectedBoard!=null) {
			if(!selectedBoard.undo(selectedBoard.getVersion())) {
				// some other peer modified the board in between
				drawSelectedWhiteboard();
			} else {
				drawSelectedWhiteboard();
				if (selectedBoard.isRemote()){
					String host = getIP(selectedBoard.getName());
					String port = Integer.toString(getPort(selectedBoard.getName())) ;
					String IPandPort = host+":"+port;
					Endpoint endpoint = peerEndpoints.get(selectedBoard.getName());
					endpoint.emit(boardUndoUpdate,selectedBoard.toString());
				}
				else{
					ArrayList<Endpoint> endpointList = boardListenedByPeer.get(selectedBoard.getName());
					for(int i = 0; i < endpointList.size(); i++){
						Endpoint endpoint = endpointList.get(i);
						endpoint.emit(boardUndoUpdate,selectedBoard.toString());
					}
				}
			}
		} else {
			log.severe("undo without a selected board");
		}
	}
	
	/**
	 * The variable selectedBoard has been set.
	 */
	public void selectedABoard() {
		
		log.info("selected board: "+selectedBoard.getName());
                if (selectedBoard.isRemote()){
//                        System.out.println("selectedboard name in selectedaboard"+ selectedBoard.getName());
                        this.getInitialBoardData(peerManager, selectedBoard.getName());
//                        System.out.println("selected board "+selectedBoard);
                }else{
                    drawSelectedWhiteboard();
                }
	}
	
	/**
	 * Set the share status on the selected board.
	 */
	public void setShare(boolean share) {
		if(selectedBoard!=null) {
                        selectedBoard.setShared(share);
                        if (!standalone){
                                if (share){
                                        System.out.println("Sharing board "+selectedBoard.getName());
                                        this.WBServerEndpoint.emit(WhiteboardServer.shareBoard,selectedBoard.getName());
                                }else{
                                        System.out.println("Unsharing board "+selectedBoard.getName());
                                        this.WBServerEndpoint.emit(WhiteboardServer.unshareBoard,selectedBoard.getName());
                                }
                        }                   
                } else {
                        log.severe("there is no selected board");
                }
	}
	
	/**
	 * Called by the gui when the user closes the app.
	 */
	
	public void guiShutdown() {
		// do some final cleanup
		HashSet<Whiteboard> existingBoards= new HashSet<>(whiteboards.values());
		existingBoards.forEach((board)->{
			deleteBoard(board.getName());
		});
                for (String boardName:boardListenedByPeer.keySet()){
                        ArrayList<Endpoint> endpointList = boardListenedByPeer.get(boardName);
                        for(int i = 0; i < endpointList.size(); i++){
                                try{
                                        Endpoint endpoint = endpointList.get(i);
                                        SessionProtocol sessionProtocol = (SessionProtocol) endpoint.getProtocol("SessionProtocol");
                                        if(sessionProtocol != null){
                                            sessionProtocol.stopSession();
                                        }
                                }catch(Exception e){
                                        System.out.println("error");
                                }

                        }
                }
                peerManager.shutdown();
                waitToFinish();
                //System.exit(0);
                
	}
	
	

	/******
	 * 
	 * GUI methods and callbacks from GUI for user actions.
	 * You probably do not need to modify anything below here.
	 * 
	 ******/
	
	/**
	 * Redraw the screen with the selected board
	 */
	public void drawSelectedWhiteboard() {
		drawArea.clear();
		if(selectedBoard!=null) {
			selectedBoard.draw(drawArea);
		}
	}
	
	/**
	 * Setup the Swing components and start the Swing thread, given the
	 * peer's specific information, i.e. peer:port string.
	 */
	public void show(String peerport) {
		// create main frame
		JFrame frame = new JFrame("Whiteboard Peer: "+peerport);
		Container content = frame.getContentPane();
		// set layout on content pane
		content.setLayout(new BorderLayout());
		// create draw area
		drawArea = new DrawArea(this);

		// add to content pane
		content.add(drawArea, BorderLayout.CENTER);

		// create controls to apply colors and call clear feature
		JPanel controls = new JPanel();
		controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));

		/**
		 * Action listener is called by the GUI thread.
		 */
		ActionListener actionListener = new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				if (e.getSource() == clearBtn) {
					clearedLocally();
				} else if (e.getSource() == blackBtn) {
					drawArea.setColor(Color.black);
				} else if (e.getSource() == redBtn) {
					drawArea.setColor(Color.red);
				} else if (e.getSource() == boardComboBox) {
					if(modifyingComboBox) return;
					if(boardComboBox.getSelectedIndex()==-1) return;
					String selectedBoardName=(String) boardComboBox.getSelectedItem();
//                                        System.out.println("selected board name in the combobox "+selectedBoardName);
					if(whiteboards.get(selectedBoardName)==null) {
						log.severe("selected a board that does not exist: "+selectedBoardName);
						return;
					}
					selectedBoard = whiteboards.get(selectedBoardName);
//					System.out.println("selected board after clicking on GUI "+selectedBoard);
					// remote boards can't have their shared status modified
					if(selectedBoard.isRemote()) {
						sharedCheckbox.setEnabled(false);
						sharedCheckbox.setVisible(false);
					} else {
						modifyingCheckBox=true;
						sharedCheckbox.setSelected(selectedBoard.isShared());
						modifyingCheckBox=false;
						sharedCheckbox.setEnabled(true);
						sharedCheckbox.setVisible(true);
					}
					selectedABoard();
				} else if (e.getSource() == createBoardBtn) {
					createBoard();
				} else if (e.getSource() == undoBtn) {
					if(selectedBoard==null) {
						log.severe("there is no selected board to undo");
						return;
					}
					undoLocally();
				} else if (e.getSource() == deleteBoardBtn) {
					if(selectedBoard==null) {
						log.severe("there is no selected board to delete");
						return;
					}
					deleteBoard(selectedBoard.getName());
				}
			}
		};
		
		clearBtn = new JButton("Clear Board");
		clearBtn.addActionListener(actionListener);
		clearBtn.setToolTipText("Clear the current board - clears remote copies as well");
		clearBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		blackBtn = new JButton("Black");
		blackBtn.addActionListener(actionListener);
		blackBtn.setToolTipText("Draw with black pen");
		blackBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		redBtn = new JButton("Red");
		redBtn.addActionListener(actionListener);
		redBtn.setToolTipText("Draw with red pen");
		redBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		deleteBoardBtn = new JButton("Delete Board");
		deleteBoardBtn.addActionListener(actionListener);
		deleteBoardBtn.setToolTipText("Delete the current board - only deletes the board locally");
		deleteBoardBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		createBoardBtn = new JButton("New Board");
		createBoardBtn.addActionListener(actionListener);
		createBoardBtn.setToolTipText("Create a new board - creates it locally and not shared by default");
		createBoardBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		undoBtn = new JButton("Undo");
		undoBtn.addActionListener(actionListener);
		undoBtn.setToolTipText("Remove the last path drawn on the board - triggers an undo on remote copies as well");
		undoBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		sharedCheckbox = new JCheckBox("Shared");
		sharedCheckbox.addItemListener(new ItemListener() {    
	         public void itemStateChanged(ItemEvent e) { 
	            if(!modifyingCheckBox) setShare(e.getStateChange()==1);
	         }    
	      }); 
		sharedCheckbox.setToolTipText("Toggle whether the board is shared or not - tells the whiteboard server");
		sharedCheckbox.setAlignmentX(Component.CENTER_ALIGNMENT);
		

		// create a drop list for boards to select from
		JPanel controlsNorth = new JPanel();
		boardComboBox = new JComboBox<String>();
		boardComboBox.addActionListener(actionListener);
		
		
		// add to panel
		controlsNorth.add(boardComboBox);
		controls.add(sharedCheckbox);
		controls.add(createBoardBtn);
		controls.add(deleteBoardBtn);
		controls.add(blackBtn);
		controls.add(redBtn);
		controls.add(undoBtn);
		controls.add(clearBtn);

		// add to content pane
		content.add(controls, BorderLayout.WEST);
		content.add(controlsNorth,BorderLayout.NORTH);

		frame.setSize(600, 600);
		
		// create an initial board
		createBoard();
		
		// closing the application
		frame.addWindowListener(new WindowAdapter() {
		    @Override
		    public void windowClosing(WindowEvent windowEvent) {
		        if (JOptionPane.showConfirmDialog(frame, 
		            "Are you sure you want to close this window?", "Close Window?", 
		            JOptionPane.YES_NO_OPTION,
		            JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION)
		        {
		        	guiShutdown();
		            frame.dispose();
		        }
		    }
		});
		
		// show the swing paint result
		frame.setVisible(true);
		
	}
	
	/**
	 * Update the GUI's list of boards. Note that this method needs to update data
	 * that the GUI is using, which should only be done on the GUI's thread, which
	 * is why invoke later is used.
	 * 
	 * @param select, board to select when list is modified or null for default
	 *                selection
	 */
	private void updateComboBox(String select) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				modifyingComboBox=true;
				boardComboBox.removeAllItems();
				int anIndex=-1;
				synchronized(whiteboards) {
					ArrayList<String> boards = new ArrayList<String>(whiteboards.keySet());
					Collections.sort(boards);
					for(int i=0;i<boards.size();i++) {
						String boardname=boards.get(i);
						boardComboBox.addItem(boardname);
						if(select!=null && select.equals(boardname)) {
							anIndex=i;
						} else if(anIndex==-1 && selectedBoard!=null && 
								selectedBoard.getName().equals(boardname)) {
							anIndex=i;
						} 
					}
				}
				modifyingComboBox=false;
				if(anIndex!=-1) {
					boardComboBox.setSelectedIndex(anIndex);
				} else {
					if(whiteboards.size()>0) {
						boardComboBox.setSelectedIndex(0);
					} else {
						drawArea.clear();
						createBoard();
					}
				}
				
			}
		});
	}
	
}
