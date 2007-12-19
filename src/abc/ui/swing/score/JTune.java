package abc.ui.swing.score;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.QuadCurve2D;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Vector;

import abc.notation.BarLine;
import abc.notation.KeySignature;
import abc.notation.MultiNote;
import abc.notation.Note;
import abc.notation.NoteAbstract;
import abc.notation.RepeatBarLine;
import abc.notation.ScoreElementInterface;
import abc.notation.StaffEndOfLine;
import abc.notation.TimeSignature;
import abc.notation.Tune;
import abc.notation.Tuplet;
import abc.notation.TwoNotesLink;
import abc.notation.Tune.Score;
import abc.ui.swing.JScoreElement;

/**
 * This class role is to render properly a tune using Java 2D.
 * @see #setTune(Tune)   
 */
public class JTune extends JScoreElement {
	/** The tune to be displayed. */
	protected Tune m_tune = null; 
	/** Hashmap that associates ScoreElement instances (key) and JScoreElement instances(value) */
	protected Hashtable m_scoreElements = null;
	/** Note instances starting Slurs and ties. */
	protected Vector m_beginningNotesLinkElements = null;
	
	private int m_staffLinesOffset = -1;
	/** The dimensions of this score. */
	//protected Dimension m_dimension = null;
	/** WTF ??? does not seem to be really taken into account anyway... */
	private int XOffset = 0;
	/** The staff lines drawings. */
	protected Vector m_staffLines = null;
	/** <TT>true</TT> if the rendition of the score should be justified, 
	 * <TT>false</TT> otherwise. */
	protected boolean m_isJustified = false;
	
	protected double m_height = -1;
	
	//temporary variables used only to cumpute the score when tune is set.
	private boolean currentStaffLineInitialized = false;
	private StaffLine currentStaffLine = null;
	private KeySignature currentKey = null;
	private TimeSignature currentTime = null;
	private Point2D cursor = null;
	
	public JTune(Tune tune, Point2D base, ScoreMetrics c) {
		this(tune, base, c, false);
	}
	
	public JTune(Tune tune, Point2D base, ScoreMetrics c, boolean isJustified) {
		super(c);
		m_staffLines = new Vector();
		m_isJustified = isJustified;
		m_scoreElements = new Hashtable();
		m_beginningNotesLinkElements = new Vector();
		setTune(tune);
		setBase(base);
	}
	
	public void onBaseChanged() {
		
	}
	
	public ScoreElementInterface getScoreElement() {
		return null;
	}
	
	public double getHeight() {
		return m_height;
	}
	
	public Tune getTune() {
		return m_tune;
	}
	
	/** Returns the hashtable that maps pure music objects to their corresponding 
	 * rendition objects.
	 * @return Returns the hashtable that maps pure music objects (ScoreElement instances 
	 * from abc.notation.* package) to rendition objects (JScoreElement instances from
	 * abc.ui.swing.score.* package) */
	public Hashtable getRenditionObjectsMapping() {
		return m_scoreElements;
	}
	
	/** Returns the part of the score (as a JScoreElement instance) located
	 * at a given point. 
	 * @param location A location as a point.
	 * @return The part of the score (as a JScoreElement instance) located
	 * at a given point. <TT>NULL</NULL> is returned if not JScoreElement is 
	 * matching the location.
	 */
	public JScoreElement getScoreElementAt(Point location) {
		JScoreElement scoreEl = null;
		for (int i=0; i<m_staffLines.size(); i++) {
			scoreEl = ((StaffLine)m_staffLines.elementAt(i)).getScoreElementAt(location);
			if (scoreEl!=null)
				return scoreEl;
			scoreEl = null;
		}
		return scoreEl;
	}
	
	/** Sets the tune to be renderered.
	 * @param tune The tune to be displayed. */
	public void setTune(Tune tune){
		m_tune = tune;
		m_scoreElements.clear();
		m_staffLines.removeAllElements();
		Score score = tune.getScore();
		//The spacing between two staff lines
		m_staffLinesOffset = (int)(m_metrics.getStaffCharBounds().getHeight()*2.5);
		cursor = new Point(XOffset, 0);
		double componentWidth =0, componentHeight = 0;
		ArrayList lessThanQuarter = new ArrayList();
		//int durationInGroup = 0;
		int maxDurationInGroup = Note.QUARTER;
		//int durationInCurrentMeasure = 0;
		Tuplet tupletContainer = null;
		int staffLineNb = 0;
		//init attributes that are for iterating through the score of the tune.
		currentKey = tune.getKey();
		currentTime = null;
		currentStaffLineInitialized = false;
		currentStaffLine = null;
		for (int i=0; i<score.size(); i++) {
			ScoreElementInterface s = (ScoreElementInterface)score.elementAt(i);
			if (
					(!(s instanceof Note)  
					|| (s instanceof Note && ((Note)s).isRest()) 
					//if we were in a tuplet and the current note isn't part of tuplet anymore or part of another tuplet
					|| (s instanceof NoteAbstract && tupletContainer!=null && (!tupletContainer.equals(((NoteAbstract)s).getTuplet())))
					//if we weren't in a tuplet and the new note is part of a tuplet.
					|| (s instanceof NoteAbstract && tupletContainer==null && ((NoteAbstract)s).isPartOfTuplet())
					|| (s instanceof Note && ((Note)s).getStrictDuration()>=Note.QUARTER)
					//TODO limitation for now, chords cannot be part of groups.
					|| (s instanceof MultiNote))
					&& lessThanQuarter.size()!=0) {
				//this is is the end of the group, append the current group content to the score.
				appendToScore(lessThanQuarter);
				lessThanQuarter.clear();
			}
			if (s instanceof KeySignature) 
				currentKey = (KeySignature)s;
			else
				if (s instanceof TimeSignature) {
					currentTime = (TimeSignature)s;
					if(s.equals(TimeSignature.SIGNATURE_4_4)) maxDurationInGroup = 2*Note.QUARTER;
					else if(((TimeSignature)s).getDenominator()==8) maxDurationInGroup = 3*Note.EIGHTH;
						else if(((TimeSignature)s).getDenominator()==4) maxDurationInGroup = Note.QUARTER;
						/*else if(s.equals(TimeSignature.SIGNATURE_3_4)) maxDurationInGroup = Note.QUARTER;*/
				}
				else
					if (s instanceof MultiNote) {
						if (((MultiNote)s).isLastOfGroup())
							System.out.println("the note " + s + " is the last of the group");
						appendToScore(new JChord((MultiNote)s, m_metrics,cursor));
						//durationInCurrentMeasure+=((MultiNote)s).getLongestNote().getDuration();
					}
					else
					if (s instanceof Note) {
						Note note = (Note)s;
						if (note.isLastOfGroup())
							System.out.println("the note " + s + " is the last of the group");
						if (note.isBeginingSlur() || note.isBeginningTie())
							m_beginningNotesLinkElements.addElement(note);
						short strictDur = note.getStrictDuration();
						tupletContainer = note.getTuplet();
						// checks if this note should be part of a group.
						if (strictDur<Note.QUARTER && !note.isRest()) {
							//durationInGroup+=(note).getDuration();
							//System.out.println("duration in group " + durationInGroup);
							lessThanQuarter.add(note);
							/*if (durationInGroup>=maxDurationInGroup) {
								appendToScore(lessThanQuarter);
								lessThanQuarter.clear();
								durationInGroup = 0;
							}*/
						}
						else {
							
							JNote noteR = new JNote(note, cursor, m_metrics);
							if (note.getHeight()>Note.c)
								noteR.setStemUp(false);
							appendToScore(noteR);
						}
						//durationInCurrentMeasure+=note.getDuration();
					}
					else
						if (s instanceof RepeatBarLine) {
							appendToScore(new JRepeatBar((RepeatBarLine)s, cursor, m_metrics));
							//durationInCurrentMeasure=0;
						}
						else
						if (s instanceof BarLine) {
							appendToScore(new JBar((BarLine)s, cursor, m_metrics));
							//durationInCurrentMeasure=0;
						}
						else
							if (s instanceof StaffEndOfLine) {
								//renderStaffLines(g2, cursor);
								staffLineNb++;
								if (cursor.getX()>componentWidth)
									componentWidth = (int)cursor.getX(); 
								/*cursor.setLocation(0, cursor.getY()+staffLinesOffset);*/
								//initNewStaffLine(currentKey, cursor, m_metrics);
								currentStaffLineInitialized = false;
							}
			if (/*
					//detects the end of a group.
					(!(s instanceof Note)  
					|| (s instanceof Note && ((Note)s).isRest()) 
					//if we were in a tuplet and the current note isn't part of tuplet anymore or part of another tuplet
					|| (s instanceof NoteAbstract && tupletContainer!=null && (!tupletContainer.equals(((NoteAbstract)s).getTuplet())))
					//if we weren't in a tuplet and the new note is part of a tuplet.
					|| (s instanceof NoteAbstract && tupletContainer==null && ((NoteAbstract)s).isPartOfTuplet())
					|| (s instanceof Note && ((Note)s).getStrictDuration()>=Note.QUARTER)
					|| (durationInCurrentMeasure!=0 && durationInCurrentMeasure%maxDurationInGroup==0)
					//TODO limitation for now, chords cannot be part of groups.
					|| (s instanceof MultiNote))
					&& lessThanQuarter.size()!=0*/
					(s instanceof NoteAbstract) && ((NoteAbstract)s).isLastOfGroup()
				) {
				//this is is the end of the group, append the current group content to the score.
				appendToScore(lessThanQuarter);
				lessThanQuarter.clear();
				//durationInGroup = 0;
			}
		}// Enf of score elements iteration.
		if (lessThanQuarter.size()!=0) {
			appendToScore(lessThanQuarter);
			lessThanQuarter.clear();
			//durationInGroup = 0;
		}
		if (cursor.getX()>componentWidth)
			componentWidth = (int)cursor.getX();
		componentHeight = (int)cursor.getY();
		
		m_width = componentWidth+m_metrics.getStaffCharBounds().getWidth();
		m_height = componentHeight+m_metrics.getStaffCharBounds().getHeight();
		
		if (m_isJustified)
			justify();
		
	}
	
	protected void appendToScore(JScoreElement element) {
		if (!currentStaffLineInitialized) {
			currentStaffLine=initNewStaffLine();
			m_staffLines.addElement(currentStaffLine);
			//currentStaffLine=initNewStaffLine(currentKey, null, cursor, m_metrics);
			currentStaffLineInitialized = true;
			element.setBase(cursor);
		}
		//element.setBase(cursor);
		currentStaffLine.addElement(element);
		double width = element.getWidth();
		int cursorNewLocationX = (int)(cursor.getX() + width + m_metrics.getNotesSpacing());
		cursor.setLocation(cursorNewLocationX, cursor.getY());
		if (element instanceof JNote)
			m_scoreElements.put(((JNote)element).getScoreElement(), element);
		else
			if (element instanceof GroupOfNotesRenderer) {
				GroupOfNotesRenderer g = (GroupOfNotesRenderer)element;
				for (int j=0; j<g.getScoreElements().length; j++)
					m_scoreElements.put(g.getScoreElements()[j], g.getRenditionElements()[j]);
			}
	}
	
	private void appendToScore(ArrayList lessThanQuarterGroup){
		JScoreElement renditionResult = null;
		JScoreElement[] renditionResultRootsElmts = new JScoreElement[lessThanQuarterGroup.size()];
		Note[] notes = (Note[])lessThanQuarterGroup.toArray(new Note[lessThanQuarterGroup.size()]);
		
			if (notes.length==1) {
				renditionResult = new JNote(notes[0], cursor, m_metrics);
				renditionResultRootsElmts[0] = renditionResult;
			}
			else {
				renditionResult = new GroupOfNotesRenderer(m_metrics, cursor, notes);
				renditionResultRootsElmts = ((GroupOfNotesRenderer)renditionResult).getRenditionElements();
			}
		appendToScore(renditionResult);
	}
	
	public double render(Graphics2D g2) {
		g2.setFont(m_metrics.getFont());
		g2.setColor(Color.BLACK);
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				
		// staff line width
		int staffCharNb = (int)(m_width/m_metrics.getStaffCharBounds().getWidth());
		char[] staffS = new char[staffCharNb+1];
		for (int i=0; i<staffS.length; i++)
			staffS[i] = ScoreMetrics.STAFF_SIX_LINES;
				
		StaffLine currentStaffLine = null;
		for (int i=0; i<m_staffLines.size(); i++) {
			currentStaffLine = (StaffLine)m_staffLines.elementAt(i);
			currentStaffLine.render(g2);
			g2.drawChars(staffS, 0, staffS.length, 0, (int)(currentStaffLine.getBase().getY()));
		}
		renderSlursAndTies(g2);
		return m_width;
	}
	
	/** Triggers the re computation of all staff lines elements in order to 
	 * get the alignment justified. */
	protected void justify() {
		if (m_staffLines.size()>0) {
			double maxWidth = ((StaffLine)m_staffLines.elementAt(0)).getWidth();
			for (int i=1; i<m_staffLines.size();i++){
				StaffLine currentStaffLine = (StaffLine)m_staffLines.elementAt(i);
				if (currentStaffLine.getWidth()>maxWidth)
					maxWidth = currentStaffLine.getWidth();
			}
			for (int i=0; i<m_staffLines.size();i++) {
				StaffLine currentStaffLine = (StaffLine)m_staffLines.elementAt(i);
				if (currentStaffLine.getWidth()>maxWidth/2)
					currentStaffLine.scaleToWidth(maxWidth);
			}
		}
	}
	
	protected void renderSlursAndTies(Graphics2D g2) {
		for (int j=0; j<m_beginningNotesLinkElements.size(); j++) {
			NoteAbstract n = (NoteAbstract)m_beginningNotesLinkElements.elementAt(j);
			TwoNotesLink link = n.getSlurDefinition() ;
			if (link==null)
				link = ((Note)n).getTieDefinition();
			if (link.getEnd()!=null)
				drawLinkDown(g2, link);
		}
	}
	
	protected void drawLinkDown(Graphics2D g2, TwoNotesLink slurDef) {
		JNote elmtStart =  (JNote)m_scoreElements.get(slurDef.getStart());
		if (slurDef.getEnd()!=null){
			JNote elmtEnd =  (JNote)m_scoreElements.get(slurDef.getEnd());
			if (elmtStart.getStaffLine().equals(elmtEnd.getStaffLine())) {
				Point2D controlPoint = null;
				
				Note lowestNote = m_tune.getScore().getLowestNoteBewteen(slurDef.getStart(), slurDef.getEnd());
				if (lowestNote.equals(slurDef.getStart()))
					controlPoint = new Point2D.Double(
							elmtStart.getSlurDownAnchor().getX()+ (elmtEnd.getSlurDownAnchor().getX()-elmtStart.getSlurDownAnchor().getX())/2,
							elmtStart.getSlurDownAnchor().getY()+ m_metrics.getNoteWidth()/4);
				else
					if (lowestNote.equals(slurDef.getEnd()))
						controlPoint = new Point2D.Double(
								elmtStart.getSlurDownAnchor().getX()+ (elmtEnd.getSlurDownAnchor().getX()-elmtStart.getSlurDownAnchor().getX())/2,
								elmtEnd.getSlurDownAnchor().getY()+ m_metrics.getNoteWidth()/4);
				
				else {
					JNote lowestNoteGlyph = (JNote)m_scoreElements.get(lowestNote);
					controlPoint = new Point2D.Double(lowestNoteGlyph.getSlurDownAnchor().getX(), 
							lowestNoteGlyph.getSlurDownAnchor().getY() + m_metrics.getNoteWidth()/2);
				}
				GeneralPath path = new GeneralPath();
				path.moveTo((int)elmtStart.getSlurDownAnchor().getX(), (int)elmtStart.getSlurDownAnchor().getY());
				QuadCurve2D q = new QuadCurve2D.Float();
				q.setCurve(
						elmtStart.getSlurDownAnchor(),
						newControl(elmtStart.getSlurDownAnchor(), controlPoint, elmtEnd.getSlurDownAnchor()), 
						elmtEnd.getSlurDownAnchor());
				path.append(q, true);
				q = new QuadCurve2D.Float();
				controlPoint.setLocation(controlPoint.getX(), controlPoint.getY()+m_metrics.getNoteWidth()/8);
				q.setCurve(
						elmtEnd.getSlurDownAnchor(),
						newControl(elmtStart.getSlurDownAnchor(), controlPoint, elmtEnd.getSlurDownAnchor()), 
						elmtStart.getSlurDownAnchor());
				path.append(q, true);
				
				g2.fill(path);
				g2.draw(path);
			}
			else
				System.err.println("Warning - ab4j limitation : Slurs / ties cannot be drawn accross several lines for now.");
		}
	}
	
	private StaffLine initNewStaffLine() {
		StaffLine sl = new StaffLine(cursor, m_metrics);
		//Vector initElements = new Vector();
		cursor.setLocation(0, cursor.getY() + m_staffLinesOffset);
		JClef clef = new JClef(cursor, m_metrics);
		sl.addElement(clef);
		//initElements.addElement(clef);
		double width = clef.getWidth();
		cursor.setLocation(cursor.getX()+width, cursor.getY());
		if (currentKey!=null) {
			JKeySignature sk = new JKeySignature(currentKey, cursor, m_metrics);
			sl.addElement(sk);
			//initElements.addElement(sk);
			width = sk.getWidth();
			int cursorNewLocationX = (int)(cursor.getX() + width + m_metrics.getNotesSpacing());
			cursor.setLocation(cursorNewLocationX, cursor.getY());
		}
		if (currentTime!=null && m_staffLines.size()==0) {
			JTimeSignature sig = new JTimeSignature(currentTime, cursor, m_metrics);
			sl.addElement(sig);
			//initElements.addElement(sig);
			width = (int)sig.getWidth();
			int cursorNewLocationX = (int)(cursor.getX() + width + m_metrics.getNotesSpacing());
			cursor.setLocation(cursorNewLocationX, cursor.getY());
		}
		//SRenderer[] initElementsAsArray = new SRenderer[initElements.size()];
		//initElements.toArray(initElementsAsArray);
		return sl;//new StaffLine(cursor, metrix, initElementsAsArray);
	}
	
	/**
	 * implementation found at 
	 * http://forum.java.sun.com/thread.jspa?threadID=609888&messageID=3362448
	 * This enables the bezier curve to be tangent to the control point.
	 */
	private Point2D newControl (Point2D start, Point2D ctrl, Point2D end) {
	        Point2D.Double newCtrl = new Point2D.Double();
	        newCtrl.x = 2 * ctrl.getX() - (start.getX() + end.getX()) / 2;
	        newCtrl.y = 2 * ctrl.getY() - (start.getY() + end.getY()) / 2;
	        return newCtrl;
	    }


}
