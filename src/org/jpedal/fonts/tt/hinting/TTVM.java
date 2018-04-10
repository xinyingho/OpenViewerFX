/*
 * ===========================================
 * Java Pdf Extraction Decoding Access Library
 * ===========================================
 *
 * Project Info:  http://www.idrsolutions.com
 * Help section for developers at http://www.idrsolutions.com/support/
 *
 * (C) Copyright 1997-2015 IDRsolutions and Contributors.
 *
 * This file is part of JPedal/JPDF2HTML5
 *
     This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.

    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA


 *
 * ---------------
 * TTVM.java
 * ---------------
 */
package org.jpedal.fonts.tt.hinting;

import org.jpedal.fonts.tt.BaseTTGlyph;
import org.jpedal.fonts.tt.FontFile2;
import org.jpedal.fonts.tt.Maxp;
import org.jpedal.utils.LogWriter;

import javax.swing.*;
import java.awt.*;
import java.io.Serializable;
import java.util.HashMap;

import java.awt.geom.Path2D;
import java.text.NumberFormat;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.lang.reflect.Field;

public class TTVM implements Serializable {
    protected static final int TWILIGHT_ZONE = 0;
    protected static final int GLYPH_ZONE = 1;
    protected static final int ORIGINAL = 2;                //Use as modifier (GLYPH_ZONE + ORIGINAL)

    //debug flags

    /**
     * Activates the Hinting Debugger. This lets you step through the font program of a glyph, seeing what's
     * done exactly where. Again, I'd recommend isolating the glyph in TTGlyph's constructor before using this flag.
     */
    private static final boolean showDebugWindow = false;

    /**
     * Enabling this flag will print the instructions before executing the glyph program. As it executes it will also
     * print out the functions it's calling. I'd recommend you isolate the glyph you're looking at in TTGlyph's
     * constructor first!
     */
    private static final boolean printGlyphInstructions = false;

    /**
     * This flag prints out the line number, name of instruction just executed, and a list of the coordinates of each
     * point in the glyph as it is now and in the original outline. The format is designed to be pasted into a
     * spreadsheet - the first column is the point number, then it's current x and y, then original x and y. Use a
     * scatter graph to see what's happening. Again, I'd recommend isolating the glyph in TTGlyph's constructor before
     * using this flag!
     */
     private static final boolean printCoordsAfterEachInstruction = false;

    /**
     * This flag makes it such that the full coordinates are printed out immediately after the watch point is changed.
     * In order to place this in context the glyph program's execution path is printed with it. Again, I'd recommend
     * isolating the glyph in TTGlyph's constructor before using this flag.
     */
    
    private static boolean watchAPoint = true;
    static {
        watchAPoint = false;//off by default
    }
    
    private static final int watchPoint=53;
    private int watchX, watchY;

    //Variables used by debugger
    private int debugPointer, instructionsExecuted, functionsLineCount;
    private int[] programToDebug;
    private boolean[] programToDebugIsData;
    private JFrame debugWindow;
    private TTGraphicsState dGS;
    private JComponent stateDisplay,debugGlyphDisplay;
    private JCheckBox showInterpolatedShadow;
    private boolean stepInto, debuggerRunningInBackground;
    private static JList currentInstructionList,stackList,cvtList,storageList;
    private JLabel currentCode, debugXLabel, debugYLabel;
    private final java.util.Stack<int[]> codeStack = new java.util.Stack();
    private final java.util.Stack<Integer> numberStack = new java.util.Stack();
    
    /**
     * Prints the name and a brief description of an instruction when it is executed.
     */
    private boolean printOut;



    //Font-wide programs
    private final int[] preProgram;
    private final int[] fontProgram;
    private boolean fontProgramRun;
    private boolean scalerRun;

    //Scaling values
    private double ptSize;
    private double ppem;
    private double scaler;

    //These will currently always be false since we generate once irrespective of the transform
//    private final boolean isRotated=false;
//    private final boolean isStretched=false;

    //Arrays for holding the data of the current glyph
    private final int[][] x, y;
    private final boolean[][] curve, contour;
    private boolean[][][] touched;

    //Information about the maximum profile of various structures
    final Maxp maxp;

    //Flag for instrctrl telling a glyph to use the default graphics state
    private boolean useDefaultGS;

    /**
     * Data structures
     */
    //Stack containing each programs working data structures
    private Stack stack;

    //Control value table - contains values which are scaled with the font and used for aligning points
    private final Cvt cvt;

    //Font-wide copy of the graphics state, which may be copied and changed before being used by a glyph!
    private final TTGraphicsState graphicsState;

    //Area for storing generic values
    private final int[] storage;

    //A map of user defined int[] functions with a number as a key
    private final HashMap<Integer, int[]> functions;

    //A map of user defined int[] functions with an opcode value as a key
    private final HashMap<Integer, int[]> instructions;


    /**
     * Instruction Opcodes
     */
    private static final int SVTCAy = 0x00;
    private static final int SVTCAx = 0x01;
    private static final int SPVTCAy = 0x02;
    private static final int SPVTCAx = 0x03;
    private static final int SFVTCAy = 0x04;
    private static final int SFVTCAx = 0x05;
    private static final int SPVTL0 = 0x06;
    private static final int SPVTL1 = 0x07;
    private static final int SFVTL0 = 0x08;
    private static final int SFVTL1 = 0x09;
    private static final int SPVFS = 0x0A;
    private static final int SFVFS = 0x0B;
    private static final int GPV = 0x0C;
    private static final int GFV = 0x0D;
    private static final int SFVTPV = 0x0E;
    private static final int ISECT = 0x0F;

    private static final int SRP0 = 0x10;
    private static final int SRP1 = 0x11;
    private static final int SRP2 = 0x12;
    private static final int SZP0 = 0x13;
    private static final int SZP1 = 0x14;
    private static final int SZP2 = 0x15;
    private static final int SZPS = 0x16;
    private static final int SLOOP = 0x17;
    private static final int RTG = 0x18;
    private static final int RTHG = 0x19;
    private static final int SMD = 0x1A;
    private static final int ELSE = 0x1B;
    private static final int JMPR = 0x1C;
    private static final int SCVTCI = 0x1D;
    private static final int SSWCI = 0x1E;
    private static final int SSW = 0x1F;

    private static final int DUP = 0x20;
    private static final int POP = 0x21;
    private static final int CLEAR = 0x22;
    private static final int SWAP = 0x23;
    private static final int DEPTH = 0x24;
    private static final int CINDEX = 0x25;
    private static final int MINDEX = 0x26;
    private static final int ALIGNPTS = 0x27;
    private static final int UTP = 0x29;
    private static final int LOOPCALL = 0x2A;
    private static final int CALL = 0x2B;
    private static final int FDEF = 0x2C;
    private static final int ENDF = 0x2D;
    private static final int MDAP0 = 0x2E;
    private static final int MDAP1 = 0x2F;

    private static final int IUPy = 0x30;
    private static final int IUPx = 0x31;
    private static final int SHP0 = 0x32;
    private static final int SHP1 = 0x33;
    private static final int SHC0 = 0x34;
    private static final int SHC1 = 0x35;
    private static final int SHZ0 = 0x36;
    private static final int SHZ1 = 0x37;
    private static final int SHPIX = 0x38;
    private static final int IP = 0x39;
    private static final int MSIRP0 = 0x3A;
    private static final int MSIRP1 = 0x3B;
    private static final int ALIGNRP = 0x3C;
    private static final int RTDG = 0x3D;
    private static final int MIAP0 = 0x3E;
    private static final int MIAP1 = 0x3F;

    private static final int NPUSHB = 0x40;
    private static final int NPUSHW = 0x41;
    private static final int WS = 0x42;
    private static final int RS = 0x43;
    private static final int WCVTP = 0x44;
    private static final int RCVT = 0x45;
    private static final int GC0 = 0x46;
    private static final int GC1 = 0x47;
    private static final int SCFS = 0x48;
    private static final int MD0 = 0x49;
    private static final int MD1 = 0x4A;
    private static final int MPPEM = 0x4B;
    private static final int MPS = 0x4C;
    private static final int FLIPON = 0x4D;
    private static final int FLIPOFF = 0x4E;
    private static final int DEBUG = 0x4F;

    private static final int LT = 0x50;
    private static final int LTEQ = 0x51;
    private static final int GT = 0x52;
    private static final int GTEQ = 0x53;
    private static final int EQ = 0x54;
    private static final int NEQ = 0x55;
    private static final int ODD = 0x56;
    private static final int EVEN = 0x57;
    private static final int IF = 0x58;
    private static final int EIF = 0x59;
    private static final int AND = 0x5A;
    private static final int OR = 0x5B;
    private static final int NOT = 0x5C;
    private static final int DELTAP1 = 0x5D;
    private static final int SDB = 0x5E;
    private static final int SDS = 0x5F;

    private static final int ADD = 0x60;
    private static final int SUB = 0x61;
    private static final int DIV = 0x62;
    private static final int MUL = 0x63;
    private static final int ABS = 0x64;
    private static final int NEG = 0x65;
    private static final int FLOOR = 0x66;
    private static final int CEILING = 0x67;
    private static final int ROUND00 = 0x68;
    private static final int ROUND01 = 0x69;
    private static final int ROUND10 = 0x6A;
    private static final int ROUND11 = 0x6B;
    private static final int NROUND00 = 0x6C;
    private static final int NROUND01 = 0x6D;
    private static final int NROUND10 = 0x6E;
    private static final int NROUND11 = 0x6F;

    private static final int WCVTF = 0x70;
    private static final int DELTAP2 = 0x71;
    private static final int DELTAP3 = 0x72;
    private static final int DELTAC1 = 0x73;
    private static final int DELTAC2 = 0x74;
    private static final int DELTAC3 = 0x75;
    private static final int SROUND = 0x76;
    private static final int S45ROUND = 0x77;
    private static final int JROT = 0x78;
    private static final int JROF = 0x79;
    private static final int ROFF = 0x7A;
    private static final int RUTG = 0x7C;
    private static final int RDTG = 0x7D;
    private static final int SANGW = 0x7E;
    private static final int AA = 0x7F;

    private static final int FLIPPT = 0x80;
    private static final int FLIPRGON = 0x81;
    private static final int FLIPRGOFF = 0x82;
    private static final int SCANCTRL = 0x85;
    private static final int SDPVTL0 = 0x86;
    private static final int SDPVTL1 = 0x87;
    private static final int GETINFO = 0x88;
    private static final int IDEF = 0x89;
    private static final int ROLL = 0x8A;
    private static final int MAX = 0x8B;
    private static final int MIN = 0x8C;
    private static final int SCANTYPE = 0x8D;
    private static final int INSTCTRL = 0x8E;

    private static final int PUSHB = 0xB0;
    private static final int PUSHW = 0xB8;

    private static final int MDRP = 0xC0;
    private static final int MIRP = 0xE0;

    private static final String[] OPCODE_DESCRIPTIONS = {
            "SVTCAy    - Set both vectors to y",
            "SVTCAx    - Set both vectors to x",
            "SPVTCAy   - Sets projection vector to y",
            "SPVTCAx   - Sets projection vector to x",
            "SFVTCAy   - Sets freedom vector to y",
            "SFVTCAx   - Sets freedom vector to x",
            "SPVTL0    - Set projection vector to line",
            "SPVTL1    - Set projection vector perpendicular to line",
            "SFVTL0    - Set freedom vector to line",
            "SFVTL1    - Set freedom vector perpendicular to line",
            "SPVFS     - Sets the projection vector from the stack",
            "SFVFS     - Sets the freedom vector from the stack",
            "GPV       - Gets the projection vector onto the stack",
            "GFV       - Gets the freedom vector onto the stack",
            "SFVTPV    - Sets freedom vector to projection vector",
            "ISECT     - Set point to intersection of lines",
            "SRP0      - Set rp0",
            "SRP1      - Set rp1",
            "SRP2      - Set rp2",
            "SZP0      - Sets zp0",
            "SZP1      - Sets zp1",
            "SZP2      - Sets zp2",
            "SZPS      - Sets all zone pointers",
            "SLOOP     - Sets loop variable",
            "RTG       - Sets round state to grid",
            "RTHG      - Sets round state to half grid",
            "SMD       - Sets minimum distance",
            "ELSE      - ELSE",
            "JMPR      - Jump",
            "SCVTCI    - Set control value table cut in",
            "SSWCI     - Set single width cut in",
            "SSW       - Set single width",
            "DUP       - Duplicate the top stack element",
            "POP       - Remove the top stack element",
            "CLEAR     - Clear the stack",
            "SWAP      - Swap the top two stack elements",
            "DEPTH     - Returns depth of stack",
            "CINDEX    - Copy Indexed element to top of stack",
            "MINDEX    - Move Indexed element to top of stack",
            "ALIGNPTS  - Move points along fv to average of their pv positions","",
            "UTP       - Untouch point",
            "LOOPCALL  - Call a function many times",
            "CALL      - Call a function",
            "FDEF      - Define a function",
            "ENDF      - End a function definition",
            "MDAP0     - Sets a point as touched",
            "MDAP1     - Rounds a point along the pV and marks as touched",
            "IUPy      - Interpolate untouched points in the y axis",
            "IUPx      - Interpolate untouched points on the x axis",
            "SHP0      - Shift point using RP2",
            "SHP1      - Shift point using RP1",
            "SHC0      - Shift a contour using RP2",
            "SHC1      - Shift a contour using RP1",
            "SHZ0      - Shift a zone using RP2",
            "SHZ1      - Shift a zone using RP1",
            "SHPIX     - Move point along freedom vector",
            "IP        - Interpolate point",
            "MSIRP0    - Move stack indirect relative point",
            "MSIRP1    - Move stack indirect relative point",
            "ALIGNRP   - Align point to RP0",
            "RTDG      - Sets round state to double grid",
            "MIAP0     - Move point to CVT value",
            "MIAP1     - Move point to CVT using cut in and round",
            "NPUSHB    - Push N bytes from IS to stack",
            "NPUSHW    - Push N words from IS to stack",
            "WS        - Write Store",
            "RS        - Read Store",
            "WCVTP     - Write Control Value Table in Pixels",
            "RCVT      - Read Control Value Table",
            "GC0       - Get coords on the pv",
            "GC1       - Get original coords on the pv",
            "SCFS",
            "MD0       - Measure current distance",
            "MD1       - Measure original distance",
            "MPPEM     - Measure pixels per em in the direction of the projection vector",
            "MPS",
            "FLIPON    - Sets autoflip to true",
            "FLIPOFF   - Sets autoflip to false",
            "DEBUG     - Shouldn't be in live fonts",
            "LT        - Less Than",
            "LTEQ      - Less Than or Equal",
            "GT        - Greater Than",
            "GTEQ      - Greater Than or Equal",
            "EQ        - Equal",
            "NEQ       - Not Equal",
            "ODD       - Rounds, truncates, and returns if odd.",
            "EVEN      - Rounds, truncates, and returns if even",
            "IF        - IF",
            "EIF       - End IF",
            "AND       - Logical AND",
            "OR        - Logical OR",
            "NOT       - Logical NOT",
            "DELTAP1   - Delta exception p1",
            "SDB       - Set delta base",
            "SDS       - Set delta shift",
            "ADD       - Add two F26Dot6 numbers",
            "SUB       - Subtract a number from another",
            "DIV       - Divide two F26Dot6 numbers",
            "MUL       - Multiply two F26Dot6 numbers",
            "ABS       - Return the absolute value of a F26Dot6 number",
            "NEG       - Negate a number",
            "FLOOR     - Round a number down if it has a fractional component",
            "CEILING   - Round a number up if it has a fractional component",
            "ROUND00   - Round a number",
            "ROUND01   - Round a number",
            "ROUND10   - Round a number",
            "ROUND11   - Round a number",
            "NROUND00  - Compensate for engine characteristics",
            "NROUND01  - Compensate for engine characteristics",
            "NROUND10  - Compensate for engine characteristics",
            "NROUND11  - Compensate for engine characteristics",
            "WCVTF",
            "DELTAP2   - Delta exception p2",
            "DELTAP3   - Delta exception p3",
            "DELTAC1   - Delta exception c1",
            "DELTAC2   - Delta exception c2",
            "DELTAC3   - Delta exception c3",
            "SROUND    - Sets the roundState specifically",
            "S45ROUND  - Sets the round state for working at 45degrees",
            "JROT      - Jump Relative On True",
            "JROF      - Jump Relative On False",
            "ROFF      - Set round state to off","",
            "RUTG      - Set round state to up to grid",
            "RDTG      - Set round state to down to grid",
            "SANGW     - deprecated",
            "AA        - deprecated",
            "FLIPPT    - Flips a number of points on/off the curve",
            "FLIPRGON  - Flips a range of points onto the curve",
            "FLIPRGOFF - Flips a range of points off the curve", "", "",
            "SCANCTRL  - We don't scan convert, so only pops a value",
            "SDPVTL0   - Sets dual projection vector to line",
            "SDPVTL1   - Sets dual projection vector perpendicular to line",
            "GETINFO   - Gets info about current glyph & font engine",
            "IDEF      - Define an instruction",
            "ROLL      - Roll the top three stack elements",
            "MAX       - Returns the maximum of two values",
            "MIN       - Returns the minimum of two values",
            "SCANTYPE  - We don't scan convert, so only pops a value",
            "INSTCTRL  - Allows for setting flags to do with glyph execution"
    };
    
    //parameters for MDRP/MIRP
    private static final int paramRESETRP0 = 16;
    private static final int paramUSEMINDIST = 8;
    private static final int paramROUND = 4;

    public TTVM(final FontFile2 currentFontFile, final Maxp maxp) {
        stack = new Stack();
        cvt = new Cvt(currentFontFile);
        graphicsState = new TTGraphicsState();

        preProgram = readProgramTable(currentFontFile, FontFile2.PREP);
        fontProgram= readProgramTable(currentFontFile, FontFile2.FPGM);

        storage = new int[maxp.getMaxStorage()];
        functions = new HashMap();
        instructions = new HashMap();

        this.maxp = maxp;

        //For some reason some prePrograms use points, even though this shouldn't theoretically be possible... set up
        //empty arrays just in case
        final int len = maxp.getMaxPoints();
        x = new int[4][len];
        y = new int[4][len];
        curve = new boolean[2][len];
        contour = new boolean[2][len];
        touched = new boolean[4][len][2];
        x[TWILIGHT_ZONE] = new int[maxp.getMaxTwilightPoints()];
        y[TWILIGHT_ZONE] = new int[maxp.getMaxTwilightPoints()];

    }


    /**
     * Sets the scale variables for a font - if it's changed, the CVT needs to be rescaled, and the PreProgram
     * (sometimes called CVT program) must be run again. As it's always called before a glyph is processed, it's also
     * the ideal place to ensure that the font program has been run before anything else.
     * @param scaler The value to multiply any unscaled values by
     * @param ppem The number of pixels per em square
     * @param ptSize The point size of the text
     */
    public void setScaleVars(final double scaler, final double ppem, final double ptSize) {

        scalerRun = false;
        this.ppem = (int)(ppem+0.5);
        this.ptSize = ptSize;

        if (!fontProgramRun) {
            execute(fontProgram, graphicsState);   //Defines functions
            fontProgramRun=true;
        }

        if (scaler != this.scaler) {
            this.scaler = scaler;
            cvt.scale(scaler);
            execute(preProgram, graphicsState);   //Sets up scan conversion (or would if we did it), CVT and store
            scalerRun = true;
        }
    }


    /**
     * Takes the information about a glyph specified and modifies it according to the instructions provided.
     * @param instructions The instructions to execute
     * @param glyfX A list of scaled X coordinates for the points
     * @param glyfY A list of scaled Y coordinates for the points
     * @param curves Whether each point is on the curve or not
     * @param contours Whether each point is the last in a contour
     */
    public void processGlyph(final int[] instructions, final int[] glyfX, final int[] glyfY, final boolean[] curves, final boolean[] contours) {
        
        if (printCoordsAfterEachInstruction || watchAPoint) {
            printOut=true;
        }
        
        x[GLYPH_ZONE] = glyfX;
        x[ORIGINAL+GLYPH_ZONE] = new int[glyfX.length];
        System.arraycopy(x[GLYPH_ZONE],0,x[ORIGINAL+GLYPH_ZONE],0,x[GLYPH_ZONE].length);

        y[GLYPH_ZONE] = glyfY;
        y[ORIGINAL+GLYPH_ZONE] = new int[glyfY.length];
        System.arraycopy(y[GLYPH_ZONE],0,y[ORIGINAL+GLYPH_ZONE],0,y[GLYPH_ZONE].length);

        curve[GLYPH_ZONE] = curves;
        contour[GLYPH_ZONE] = contours;

        int max = maxp.getMaxTwilightPoints();
        if (glyfX.length > max) {
            max = glyfX.length;
        }

        touched = new boolean[4][max][2];

        if (printGlyphInstructions) {
            print(instructions);
        }
        
        stack = new Stack();

        //Sort out graphicsState
        TTGraphicsState gs;
        if (useDefaultGS) {
            //If INSTCTRL flag 2 set use default values for glyph instructions
            gs = new TTGraphicsState();
        } else {
            //If glyph create a copy so any changes are only for this glyph
            try {
                gs = (TTGraphicsState)graphicsState.clone();
                gs.resetForGlyph();
            } catch(final CloneNotSupportedException e) {
                LogWriter.writeLog("Exception: " + e.getMessage());
                
                gs = new TTGraphicsState();
            }
        }

        //Disable glyph instructions if previously set by INSTCTRL
        if (gs.instructControl != 0) {
            return;
        }

        //record starting position of watch point
        if (watchAPoint) {
            watchX = x[GLYPH_ZONE][watchPoint];
            watchY = y[GLYPH_ZONE][watchPoint];
        }

        if (showDebugWindow) {
            programToDebug = instructions;
            programToDebugIsData = getInstructionStreamIsData(programToDebug);
        }
        
        execute(instructions, gs);

        if (printCoordsAfterEachInstruction || watchAPoint) {
            printOut=false;
        }

        if (showDebugWindow) {
            final Thread t = new Thread("getDebugListData") {
                @Override
                public void run() {
                    runDebugger();
                }
            };
            t.start();
        }
    }


    /**
     * Execute a section of code
     * @param program The code to execute
     * @param gs The graphics state to use
     */
    private void execute(final int[] program, final TTGraphicsState gs) {
        if (program==null) {
            return;
        }

        if (showDebugWindow && stepInto && debugWindow != null && debugWindow.isVisible()) {
            codeStack.push(programToDebug);
            numberStack.push(debugPointer);
            functionsLineCount += program.length;
            setCurrentCodeForDebug(program, -1, !debuggerRunningInBackground);
            return;
        }
        
        for (int currentPointer = 0; currentPointer < program.length; currentPointer++) {
            if (printOut) {
                System.out.print(currentPointer + "\t");
            }
            currentPointer = process(program[currentPointer], currentPointer, program, gs);

            //Check for change in position of watch point
            if (watchAPoint &&
                    (watchX != x[GLYPH_ZONE][watchPoint] || watchY != y[GLYPH_ZONE][watchPoint])) {
                final int diffX = x[GLYPH_ZONE][watchPoint] - watchX;
                final int diffY = y[GLYPH_ZONE][watchPoint] - watchY;
                watchX = x[GLYPH_ZONE][watchPoint];
                watchY = y[GLYPH_ZONE][watchPoint];
                System.out.print("Changed point " + watchPoint + " (");

                if (diffX > 0) {
                    System.out.print("x+" + diffX);
                } else if (diffX != 0) {
                    System.out.print("x" + diffX);
                }

                if (diffX != 0 && diffY != 0) {
                    System.out.print(", ");
                }

                if (diffY > 0) {
                    System.out.print("y+" + diffY);
                } else if (diffY != 0) {
                    System.out.print("y" + diffY);
                }

                System.out.println(")");
                printCoords();

            }

//            if (printOut)
//                stack.print();
            if (printCoordsAfterEachInstruction && printOut) {
                printCoords();
            }
            
            //Check if errors have been encountered and cease execution if they have
            if (BaseTTGlyph.redecodePage) {
                return;
            }
        }

    }


    /**
     * Process a command
     * @param code The command to process
     * @param currentPointer The location in the program (passed in so can be modified and passed out)
     * @param program The program
     * @param gs The graphics state to use
     * @return The (possibly modified) location in the program
     */
    @SuppressWarnings("OverlyLongMethod")
    private int process(int code, int currentPointer, final int[] program, final TTGraphicsState gs) {

        //Warning supressed as originalPointer is used by debug code
        int originalPointer = currentPointer;
        
        //If it's reading data find how much to read & redirect to first command
        int bytesToRead=0;
        if (code >= 0xB0 && code <= 0xBF) {
            bytesToRead = code %8;
            code -= bytesToRead;
            bytesToRead++;
        }

        if (printOut && code < OPCODE_DESCRIPTIONS.length) {
            System.out.println(OPCODE_DESCRIPTIONS[code]);
        }
        
        try {

            switch(code) {
                case SVTCAy:
                    gs.freedomVector = TTGraphicsState.y_axis;
                    gs.projectionVector = TTGraphicsState.y_axis;
                    gs.dualProjectionVector = TTGraphicsState.y_axis;
                    break;

                case SVTCAx:
                    gs.freedomVector = TTGraphicsState.x_axis;
                    gs.projectionVector = TTGraphicsState.x_axis;
                    gs.dualProjectionVector = TTGraphicsState.x_axis;
                    break;

                case SPVTCAy:
                    gs.projectionVector = TTGraphicsState.y_axis;
                    gs.dualProjectionVector = TTGraphicsState.y_axis;
                    break;

                case SPVTCAx:
                    gs.projectionVector = TTGraphicsState.x_axis;
                    gs.dualProjectionVector = TTGraphicsState.x_axis;
                    break;

                case SFVTCAy:
                    gs.freedomVector = TTGraphicsState.y_axis;
                    break;

                case SFVTCAx:
                    gs.freedomVector = TTGraphicsState.x_axis;
                    break;

                case SPVTL0: {
                    final int p1 = stack.pop();
                    final int p2 = stack.pop();

                    //Note: The MS and Apple documentation disagree on which zone pointers to use - Apple
                    //matches Freetype so we're using that for now.

                    double xdiff = getDoubleFromF26Dot6(x[gs.zp2][p2] - x[gs.zp1][p1]);
                    double ydiff = getDoubleFromF26Dot6(y[gs.zp2][p2] - y[gs.zp1][p1]);
                    final double factor = Math.sqrt((xdiff * xdiff) + (ydiff * ydiff));
                    xdiff /= factor;
                    ydiff /= factor;
                    gs.projectionVector = TTGraphicsState.createVector(storeDoubleAsF2Dot14(xdiff), storeDoubleAsF2Dot14(ydiff));
                    gs.dualProjectionVector = gs.projectionVector;
                    break;
                }
                case SPVTL1: {
                    final int p1 = stack.pop();
                    final int p2 = stack.pop();

                    //Note: The MS and Apple documentation disagree on which zone pointers to use - Apple
                    //matches Freetype so we're using that for now.

                    double xdiff = getDoubleFromF26Dot6(x[gs.zp2][p2] - x[gs.zp1][p1]);
                    double ydiff = getDoubleFromF26Dot6(y[gs.zp2][p2] - y[gs.zp1][p1]);
                    final double factor = Math.sqrt((xdiff * xdiff) + (ydiff * ydiff));
                    xdiff /= factor;
                    ydiff /= factor;
                    gs.projectionVector = TTGraphicsState.createVector(storeDoubleAsF2Dot14(-ydiff), storeDoubleAsF2Dot14(xdiff));
                    gs.dualProjectionVector = gs.projectionVector;
                    break;
                }
                case SFVTL0: {
                    final int p1 = stack.pop();
                    final int p2 = stack.pop();
                    double xdiff = getDoubleFromF26Dot6(x[gs.zp1][p2] - x[gs.zp2][p1]);
                    double ydiff = getDoubleFromF26Dot6(y[gs.zp1][p2] - y[gs.zp2][p1]);
                    final double factor = Math.sqrt((xdiff * xdiff) + (ydiff * ydiff));
                    xdiff /= factor;
                    ydiff /= factor;
                    gs.freedomVector = TTGraphicsState.createVector(storeDoubleAsF2Dot14(xdiff), storeDoubleAsF2Dot14(ydiff));
                    break;
                }
                case SFVTL1: {
                    final int p1 = stack.pop();
                    final int p2 = stack.pop();
                    double xdiff = getDoubleFromF26Dot6(x[gs.zp1][p2] - x[gs.zp2][p1]);
                    double ydiff = getDoubleFromF26Dot6(y[gs.zp1][p2] - y[gs.zp2][p1]);
                    final double factor = Math.sqrt((xdiff * xdiff) + (ydiff * ydiff));
                    xdiff /= factor;
                    ydiff /= factor;
                    gs.freedomVector = TTGraphicsState.createVector(storeDoubleAsF2Dot14(-ydiff), storeDoubleAsF2Dot14(xdiff));
                    break;
                }
                case SPVFS: {
                    final int y = stack.pop();
                    final int x = stack.pop();
                    gs.projectionVector = TTGraphicsState.createVector(x, y);
                    gs.dualProjectionVector = gs.projectionVector;
                    break;
                }
                case SFVFS: {
                    final int y = stack.pop();
                    final int x = stack.pop();
                    gs.freedomVector = TTGraphicsState.createVector(x, y);
                    break;
                }
                case GPV: {
                    final int[] pv = TTGraphicsState.getVectorComponents(gs.projectionVector);
                    stack.push(pv[0]);
                    stack.push(pv[1]);
                    break;
                }
                case GFV: {
                    final int[] fv = TTGraphicsState.getVectorComponents(gs.freedomVector);
                    stack.push(fv[0]);
                    stack.push(fv[1]);
                    break;
                }
                case SFVTPV:
                    gs.freedomVector = gs.projectionVector;
                    break;

                case ISECT: {
                    final int b1 = stack.pop();        //line b end
                    final int b0 = stack.pop();        //line b start
                    final int a1 = stack.pop();        //line a end
                    final int a0 = stack.pop();        //line a start

                    //Note: The Apple and Microsoft specifications differ on which zone pointers to use
                    //for lines A and B. I had a look at Freetype which matched the MS documentation,
                    //so that's what we're using for now.
                    final int ax = x[gs.zp1][a0];
                    final int ay = y[gs.zp1][a0];
                    final int adx = x[gs.zp1][a1] - ax;  //change in x for line a
                    final int ady = y[gs.zp1][a1] - ay;

                    final int bx = x[gs.zp0][b0];
                    final int by = y[gs.zp0][b0];
                    final int bdx = x[gs.zp0][b1] - bx;
                    final int bdy = y[gs.zp0][b1] - by;

                    final int x;
                    final int y;
                    if (adx == 0 && bdx == 0) {       //both lines vertical
                        x = ax + bx / 2;
                        y = (ay + by + (ay + ady) + (by + bdy)) / 4;
                    } else if (adx == 0) {                                 //a vertical - use x & find b's y
                        final double bm = getDoubleFromF26Dot6(bdy) / getDoubleFromF26Dot6(bdx);
                        final double bc = getDoubleFromF26Dot6(by) - (bm * getDoubleFromF26Dot6(bx));
                        x = ax;
                        y = storeDoubleAsF26Dot6((bm * getDoubleFromF26Dot6(ax)) + bc);
                    } else if (bdx == 0) {                                 //b vertical - use x & find a's y
                        final double am = getDoubleFromF26Dot6(ady) / getDoubleFromF26Dot6(adx);
                        final double ac = getDoubleFromF26Dot6(ay) - (am * getDoubleFromF26Dot6(ax));
                        x = bx;
                        y = storeDoubleAsF26Dot6((am * getDoubleFromF26Dot6(bx)) + ac);
                    } else {                                               //neither line vertical - find mx+c form
                        final double am = getDoubleFromF26Dot6(ady) / getDoubleFromF26Dot6(adx);
                        final double ac = getDoubleFromF26Dot6(ay) - (am * getDoubleFromF26Dot6(ax));
                        final double bm = getDoubleFromF26Dot6(bdy) / getDoubleFromF26Dot6(bdx);
                        final double bc = getDoubleFromF26Dot6(by) - (bm * getDoubleFromF26Dot6(bx));
                        if (am == bm) {                                    //lines parallel
                            x = (ax + bx + (ax + adx) + (bx + bdx)) / 4;
                            y = (ay + by + (ay + ady) + (by + bdy)) / 4;
                        } else {                                           //lines intersect
                            final double fx = (bc - ac) / (am - bm);             //use rearranged mx1+c1 = mx2+c2 to find x
                            x = storeDoubleAsF26Dot6(fx);
                            y = storeDoubleAsF26Dot6((am * fx) + ac);       //substitute into y=mx+c to get y
                        }
                    }

                    //set point
                    final int p = stack.pop();
                    this.x[gs.zp2][p] = x;
                    this.y[gs.zp2][p] = y;
                    break;
                }
                case SRP0:
                    gs.rp0 = stack.pop();
                    break;

                case SRP1:
                    gs.rp1 = stack.pop();
                    break;

                case SRP2:
                    gs.rp2 = stack.pop();
                    break;

                case SZP0: {
                   // final int value = stack.pop();
//                    if (value > 1 || value < 0) {
//                        System.out.println("ZP0 set incorrectly!");
//                    }
                    
                    gs.zp0 = stack.pop();
                    break;
                }
                case SZP1: {
                    final int value = stack.pop();
                    if (value > 1 || value < 0) {
                        System.out.println("ZP1 set incorrectly!");
                    }
                    
                    gs.zp1 = value;
                    break;
                }
                case SZP2: {
                    gs.zp2 = stack.pop();
//                    if (value > 1 || value < 0) {
//                        System.out.println("ZP2 set incorrectly!");
//                    }
                    
                   // gs.zp2 = value;
                    break;
                }
                case SZPS: {
                    final int value = stack.pop();
//                    if (value > 1 || value < 0) {
//                        System.out.println("All zone pointers set incorrectly!");
//                    }
                    
                    gs.zp0 = value;
                    gs.zp1 = value;
                    gs.zp2 = value;
                    break;
                }
                case SLOOP:
                    gs.loop = stack.pop();
                    break;

                case RTG:
                    gs.roundState = TTGraphicsState.g;
                    gs.gridPeriod = 1.0;
                    break;

                case RTHG:
                    gs.roundState = TTGraphicsState.hg;
                    gs.gridPeriod = 1.0;
                    break;

                case SMD:
                    gs.minimumDistance = stack.pop();
                    break;

                case ELSE: {
                    //only processed at all if preceeding IF is true - skip until EIF
                    int curr = 0;
                    int nest = 0;
                    do {
                        //Deal with nested IF's
                        if (curr == EIF && nest != 0) {
                            nest--;
                        }

                        currentPointer++;
                        curr = program[currentPointer];

                        //deal with nested IF's
                        if (curr == IF) {
                            nest++;
                        }

                        //skip over any data in stream
                        if (curr == NPUSHB) {
                            currentPointer++;
                            currentPointer += program[currentPointer];
                        } else if (curr == NPUSHW) {
                            currentPointer++;
                            currentPointer += program[currentPointer] * 2;
                        } else if (curr >= PUSHB && curr <= PUSHB+7) {
                            currentPointer += (curr + 1) - PUSHB;
                        } else if (curr >= PUSHW && curr <= PUSHW+7) {
                            currentPointer += ((curr + 1) - PUSHW) * 2;
                        }
                    } while (curr != EIF || nest != 0);
                    break;
                }
                case JMPR: {
                    final int value = stack.pop();
                    currentPointer = (currentPointer + value) - 1;
                    if (currentPointer < 0) {
                        throw new RuntimeException("Jumped back further than the start of the instruction.");
                    }
                    break;
                }
                case SCVTCI:
                    gs.controlValueTableCutIn = stack.pop();
                    break;

                case SSWCI:
                    gs.singleWidthCutIn = stack.pop();
                    break;

                case SSW:
                    gs.singleWidthValue = stack.pop();
                    break;

                case DUP: {
                    final int value = stack.pop();
                    stack.push(value);
                    stack.push(value);
                    break;
                }
                case POP:
                    stack.pop();
                    break;

                case CLEAR:
                    stack = new Stack();
                    break;

                case SWAP: {
                    final int top = stack.pop();
                    final int under = stack.pop();
                    stack.push(top);
                    stack.push(under);
                    break;
                }
                case DEPTH: {
                    stack.push(stack.size());
                    break;
                }
                case CINDEX: {
                    final int key = stack.pop();
                    final int value = stack.elementAt(key);
                    stack.push(value);
                    break;
                }
                case MINDEX: {
                    final int key = stack.pop();
                    final int value = stack.remove(key);
                    stack.push(value);
                    break;
                }
                case ALIGNPTS: {
                    final int p1 = stack.pop();
                    final int p2 = stack.pop();
                    final int p1loc = TTGraphicsState.getCoordsOnVector(gs.projectionVector, x[gs.zp1][p1], y[gs.zp1][p1]);
                    final int p2loc = TTGraphicsState.getCoordsOnVector(gs.projectionVector, x[gs.zp0][p2], y[gs.zp0][p2]);
                    final int target = (p1loc + p2loc) / 2;
                    final int[] shift = gs.getFVMoveforPVDistance(target - p1loc);
                    x[gs.zp1][p1] += shift[0];
                    y[gs.zp1][p1] += shift[1];
                    x[gs.zp0][p2] -= shift[0];
                    y[gs.zp0][p2] -= shift[1];

                    final int[] fv = TTGraphicsState.getVectorComponents(gs.freedomVector);
                    if (fv[0] != 0) {
                        touched[gs.zp1][p1][0] = true;
                        touched[gs.zp0][p2][0] = true;
                    }
                    if (fv[1] != 0) {
                        touched[gs.zp1][p1][1] = true;
                        touched[gs.zp0][p2][1] = true;
                    }
                    break;
                }
                case UTP: {
                    final int p = stack.pop();
                    if (gs.freedomVector == TTGraphicsState.x_axis) {
                        touched[gs.zp0][p][0] = false;
                    } else if (gs.freedomVector == TTGraphicsState.y_axis) {
                        touched[gs.zp0][p][1] = false;
                    } else {
                        touched[gs.zp0][p][0] = false;
                        touched[gs.zp0][p][1] = false;
                    }
                    break;
                }
                case LOOPCALL: {
                    final int func = stack.pop();
                    final int count = stack.pop();
                    final int[] function = functions.get(Integer.valueOf(func));

                    //debug code
                    if (printGlyphInstructions && scalerRun) {
                        System.out.println("Function " + func + " on line " + currentPointer);
                        print(function);
                        System.out.println("");
                    }
                    
                    for (int i = 0; i < count; i++) {
                        execute(function, gs);
                    }
                    if (printOut) {
                        System.out.println("LOOPCALL finished");
                    }
                    break;
                }
                case CALL: {
                    final int func = stack.pop();
                    final int[] function = functions.get(Integer.valueOf(func));

                    //debug code
                    if (printGlyphInstructions && scalerRun) {
                        System.out.println("Function " + func + " on line " + currentPointer);
                        print(function);
                        System.out.println("");
                    }
                    
                    execute(function, gs);
                    if (printOut) {
                        System.out.println("CALL finished");
                    }
                    break;
                }
                case FDEF: {
                    final int num = stack.pop();
                    final int start = currentPointer;

                    //work out length
                    int curr;
                    do {
                        currentPointer++;
                        curr = program[currentPointer];

                        //skip over any data in stream
                        if (curr == NPUSHB) {
                            currentPointer++;
                            currentPointer += program[currentPointer];
                        } else if (curr == NPUSHW) {
                            currentPointer++;
                            currentPointer += program[currentPointer] * 2;
                        } else if (curr >= PUSHB && curr <= PUSHB+7) {
                            currentPointer += (curr + 1) - PUSHB;
                        } else if (curr >= PUSHW && curr <= PUSHW+7) {
                            currentPointer += ((curr + 1) - PUSHW) * 2;
                        }
                    } while (curr != ENDF);
                    final int len = (currentPointer - start) - 1;
                    currentPointer = start;

                    //create function
                    final int[] function = new int[len];
                    for (int i = 0; i < len; i++) {
                        currentPointer++;
                        function[i] = program[currentPointer];
                    }
                    functions.put(num, function);

                    //skip past ENDF
                    currentPointer++;
                    break;
                }
                case ENDF:
                    //No definition required
                    break;

                case MDAP0: {
                    final int p = stack.pop();
                    gs.rp0 = p;
                    gs.rp1 = p;
                    if (gs.freedomVector == TTGraphicsState.x_axis) {
                        touched[gs.zp0][p][0] = true;
                    } else if (gs.freedomVector == TTGraphicsState.y_axis) {
                        touched[gs.zp0][p][1] = true;
                    } else {
                        touched[gs.zp0][p][0] = true;
                        touched[gs.zp0][p][1] = true;
                    }
                    break;
                }
                case MDAP1: {
                    final int p = stack.pop();
                    gs.rp0 = p;
                    gs.rp1 = p;

                    int m = TTGraphicsState.getCoordsOnVector(gs.projectionVector, x[gs.zp0][p], y[gs.zp0][p]);
                    m = storeDoubleAsF26Dot6(gs.round(getDoubleFromF26Dot6(m))) - m;
                    final int[] shift = gs.getFVMoveforPVDistance(m);

                    x[gs.zp0][p] += shift[0];
                    y[gs.zp0][p] += shift[1];

                    final int[] fv = TTGraphicsState.getVectorComponents(gs.freedomVector);
                    if (fv[0] != 0) {
                        touched[gs.zp0][p][0] = true;
                    }
                    if (fv[1] != 0) {
                        touched[gs.zp0][p][1] = true;
                    }
                    break;
                }
                case IUPy: {
                    interpolateUntouchedPoints(IUPy);
                    break;
                }
                case IUPx: {
                    interpolateUntouchedPoints(IUPx);
                    break;
                }
                case SHP0: {
                    for (int i = 0; i < gs.loop; i++) {
                        final int p = stack.pop();

                        if (p > x[gs.zp2].length || gs.rp2 > x[gs.zp1].length) {
                            LogWriter.writeLog("Trying to use a point which doesn't exist! (SHP0, zone " + gs.zp2 + ')');
                            break;
                        }

                        final int newRP = TTGraphicsState.getCoordsOnVector(gs.projectionVector, x[gs.zp1][gs.rp2], y[gs.zp1][gs.rp2]);
                        final int oldRP = TTGraphicsState.getCoordsOnVector(gs.projectionVector, x[ORIGINAL + gs.zp1][gs.rp2], y[ORIGINAL + gs.zp1][gs.rp2]);
                        final int pMove = newRP - oldRP;
                        final int[] shift = gs.getFVMoveforPVDistance(pMove);
                        x[gs.zp2][p] += shift[0];
                        y[gs.zp2][p] += shift[1];

                        final int[] fv = TTGraphicsState.getVectorComponents(gs.freedomVector);
                        if (fv[0] != 0) {
                            touched[gs.zp2][p][0] = true;
                        }
                        if (fv[1] != 0) {
                            touched[gs.zp2][p][1] = true;
                        }
                    }
                    gs.loop = 1;
                    break;
                }
                case SHP1: {
                    for (int i = 0; i < gs.loop; i++) {
                        final int p = stack.pop();
                        final int newRP = TTGraphicsState.getCoordsOnVector(gs.projectionVector, x[gs.zp0][gs.rp1], y[gs.zp0][gs.rp1]);
                        final int oldRP = TTGraphicsState.getCoordsOnVector(gs.projectionVector, x[ORIGINAL + gs.zp0][gs.rp1], y[ORIGINAL + gs.zp0][gs.rp1]);
                        final int shift = newRP - oldRP;
                        final int[] move = gs.getFVMoveforPVDistance(shift);
                        x[gs.zp2][p] += move[0];
                        y[gs.zp2][p] += move[1];
                        if (move[0] != 0) {
                            touched[gs.zp2][p][0] = true;
                        }
                        if (move[1] != 0) {
                            touched[gs.zp2][p][1] = true;
                        }
                    }
                    gs.loop = 1;
                    break;
                }
                case SHC0: {
                    final int c = stack.pop();

                    //Note: The spec doesn't clearly say how a contour is identified - for now we are finding
                    //all of the contours and using the cth one, but another possibility is that you use the
                    //contour point c is a part of.

                    //get start and length of contours
                    final int[] contourLengths = new int[contour[GLYPH_ZONE].length];
                    final int[] contourStarts = new int[contour[GLYPH_ZONE].length];
                    int contourCount = 0, lastContour = 0;
                    contourStarts[0] = 0;
                    for (int i = 0; i < contour[GLYPH_ZONE].length; i++) {
                        if (contour[GLYPH_ZONE][i]) {
                            contourStarts[contourCount + 1] = i + 1;
                            contourLengths[contourCount] = i + 1 - lastContour;
                            lastContour = i + 1;
                            contourCount++;
                        }
                    }

                    //Get move required
                    final int newRP = TTGraphicsState.getCoordsOnVector(gs.projectionVector, x[gs.zp1][gs.rp2], y[gs.zp1][gs.rp2]);
                    final int oldRP = TTGraphicsState.getCoordsOnVector(gs.projectionVector, x[ORIGINAL + gs.zp1][gs.rp2], y[ORIGINAL + gs.zp1][gs.rp2]);
                    final int shift = newRP - oldRP;
                    final int[] move = gs.getFVMoveforPVDistance(shift);

                    //Move contour
                    for (int i = contourStarts[c]; i < contourStarts[c] + contourLengths[c]; i++) {
                        if (gs.zp1 == gs.zp2 || i != gs.rp2) {
                            x[gs.zp2][i] += move[0];
                            y[gs.zp2][i] += move[1];
                        }
                    }
                    break;
                }
                case SHC1: {
                    final int c = stack.pop();

                    //Note: The spec doesn't clearly say how a contour is identified - for now we are finding
                    //all of the contours and using the cth one, but another possibility is that you use the
                    //contour point c is a part of.

                    //get start and length of contours
                    final int[] contourLengths = new int[contour[GLYPH_ZONE].length];
                    final int[] contourStarts = new int[contour[GLYPH_ZONE].length];
                    int contourCount = 0, lastContour = 0;
                    contourStarts[0] = 0;
                    for (int i = 0; i < contour[GLYPH_ZONE].length; i++) {
                        if (contour[GLYPH_ZONE][i]) {
                            contourStarts[contourCount + 1] = i + 1;
                            contourLengths[contourCount] = i + 1 - lastContour;
                            lastContour = i + 1;
                            contourCount++;
                        }
                    }

                    //Get move required
                    final int newRP = TTGraphicsState.getCoordsOnVector(gs.projectionVector, x[gs.zp0][gs.rp1], y[gs.zp0][gs.rp1]);
                    final int oldRP = TTGraphicsState.getCoordsOnVector(gs.projectionVector, x[ORIGINAL + gs.zp0][gs.rp1], y[ORIGINAL + gs.zp0][gs.rp1]);
                    final int shift = newRP - oldRP;
                    final int[] move = gs.getFVMoveforPVDistance(shift);

                    //Move contour
                    for (int i = contourStarts[c]; i < contourStarts[c] + contourLengths[c]; i++) {
                        if (gs.zp2 != gs.zp0 || i != gs.rp1) {
                            x[gs.zp2][i] += move[0];
                            y[gs.zp2][i] += move[1];
                        }
                    }
                    break;
                }
                case SHZ0: {
                    final int z = stack.pop();

                    //Get move required
                    final int newRP = TTGraphicsState.getCoordsOnVector(gs.projectionVector, x[gs.zp1][gs.rp2], y[gs.zp1][gs.rp2]);
                    final int oldRP = TTGraphicsState.getCoordsOnVector(gs.projectionVector, x[ORIGINAL + gs.zp1][gs.rp2], y[ORIGINAL + gs.zp1][gs.rp2]);
                    final int shift = newRP - oldRP;
                    final int[] move = gs.getFVMoveforPVDistance(shift);

                    //Move zone
                    for (int i = 0; i < x[z].length; i++) {
                        if (z != gs.zp1 || i != gs.rp2) {
                            x[z][i] += move[0];
                            y[z][i] += move[1];
                        }
                    }
                    break;
                }
                case SHZ1: {
                    final int z = stack.pop();

                    //Get move required
                    final int newRP = TTGraphicsState.getCoordsOnVector(gs.projectionVector, x[gs.zp0][gs.rp1], y[gs.zp0][gs.rp1]);
                    final int oldRP = TTGraphicsState.getCoordsOnVector(gs.projectionVector, x[ORIGINAL + gs.zp0][gs.rp1], y[ORIGINAL + gs.zp0][gs.rp1]);
                    final int shift = newRP - oldRP;
                    final int[] move = gs.getFVMoveforPVDistance(shift);

                    //Move zone
                    for (int i = 0; i < x[z].length; i++) {
                        if (z != gs.zp0 || i != gs.rp1) {
                            x[z][i] += move[0];
                            y[z][i] += move[1];
                        }
                    }
                    break;
                }
                case SHPIX: {
                    final int magnitude = stack.pop();
                    final int[] fv = TTGraphicsState.getVectorComponents(gs.freedomVector);
                    for (int i = 0; i < gs.loop; i++) {
                        final int point = stack.pop();
                        x[gs.zp2][point] += (magnitude * getDoubleFromF2Dot14(fv[0])/64);
                        y[gs.zp2][point] += (magnitude * getDoubleFromF2Dot14(fv[1])/64);
                        if (fv[0] != 0) {
                            touched[gs.zp2][point][0] = true;
                        }
                        if (fv[1] != 0) {
                            touched[gs.zp2][point][1] = true;
                        }
                    }
                    gs.loop = 1;
                    break;
                }
                case IP: {
                    for (int i = 0; i < gs.loop; i++) {
                        final int p = stack.pop();

                        if (p < 0 || p > x[gs.zp2].length || gs.rp1 > x[gs.zp0].length || gs.rp2 > x[gs.zp1].length) {
                            LogWriter.writeLog("Trying to use a point which doesn't exist! (IP, zone " + gs.zp2 + ')');
                            break;
                        }

                        //work out points relationship to reference points
                        final int originalRP1 = TTGraphicsState.getCoordsOnVector(gs.dualProjectionVector, x[ORIGINAL + gs.zp0][gs.rp1], y[ORIGINAL + gs.zp0][gs.rp1]);
                        final int originalRP2 = TTGraphicsState.getCoordsOnVector(gs.dualProjectionVector, x[ORIGINAL + gs.zp1][gs.rp2], y[ORIGINAL + gs.zp1][gs.rp2]);

                        //The instruction is illegal if rp1 and rp2 occupy the same position on the projection vector
                        if (originalRP1 != originalRP2) {

                            final int originalP = TTGraphicsState.getCoordsOnVector(gs.dualProjectionVector, x[ORIGINAL + gs.zp2][p], y[ORIGINAL + gs.zp2][p]);
                            final double pos = (double) (originalP - originalRP1) / (originalRP2 - originalRP1);

                            //find move along PV required
                            final int newRP1 = TTGraphicsState.getCoordsOnVector(gs.projectionVector, x[gs.zp0][gs.rp1], y[gs.zp0][gs.rp1]);
                            final int newRP2 = TTGraphicsState.getCoordsOnVector(gs.projectionVector, x[gs.zp1][gs.rp2], y[gs.zp1][gs.rp2]);
                            final int pMove = (int) (((pos * (newRP2 - newRP1)) + newRP1) + 0.5) - originalP;

                            //calculate and apply shift
                            final int[] shift = gs.getFVMoveforPVDistance(pMove);
                            x[gs.zp2][p] += shift[0];
                            y[gs.zp2][p] += shift[1];

                            final int[] fv = TTGraphicsState.getVectorComponents(gs.freedomVector);
                            if (fv[0] != 0) {
                                touched[gs.zp2][p][0] = true;
                            }
                            if (fv[1] != 0) {
                                touched[gs.zp2][p][1] = true;
                            }
                        }
                    }
                    gs.loop = 1;
                    break;
                }
                case MSIRP0: {
                    final int d = stack.pop();
                    final int p = stack.pop();


                    //move to rp0 + d
                    final int[] shift = gs.getFVMoveforPVDistance(d -
                            (TTGraphicsState.getCoordsOnVector(gs.projectionVector,x[gs.zp1][p],y[gs.zp1][p])-TTGraphicsState.getCoordsOnVector(gs.projectionVector,x[gs.zp0][gs.rp0],y[gs.zp0][gs.rp0])));
                    x[gs.zp1][p] += shift[0];
                    y[gs.zp1][p] += shift[1];

                    //Mark as touched
                    final int[] fv = TTGraphicsState.getVectorComponents(gs.freedomVector);
                    if (fv[0] != 0) {
                        touched[gs.zp1][p][0] = true;
                    }
                    if (fv[1] != 0) {
                        touched[gs.zp1][p][1] = true;
                    }

                    //inexplicable value settings as described in the guide..
                    gs.rp1 = gs.rp0;
                    gs.rp2 = p;
                    break;
                }
                case MSIRP1: {
                    final int d = stack.pop();
                    final int p = stack.pop();

                    //move to rp0 + d
                    final int[] shift = gs.getFVMoveforPVDistance(d -
                            (TTGraphicsState.getCoordsOnVector(gs.projectionVector,x[gs.zp1][p],y[gs.zp1][p])-TTGraphicsState.getCoordsOnVector(gs.projectionVector,x[gs.zp0][gs.rp0],y[gs.zp0][gs.rp0])));
                    x[gs.zp1][p] += shift[0];
                    y[gs.zp1][p] += shift[1];

                    //Mark as touched
                    final int[] fv = TTGraphicsState.getVectorComponents(gs.freedomVector);
                    if (fv[0] != 0) {
                        touched[gs.zp1][p][0] = true;
                    }
                    if (fv[1] != 0) {
                        touched[gs.zp1][p][1] = true;
                    }

                    //inexplicable value settings as described in the guide..
                    gs.rp1 = gs.rp0;
                    gs.rp2 = p;
                    gs.rp0 = p;
                    break;
                }
                case ALIGNRP:
                    for (int i = 0; i < gs.loop; i++) {
                        final int p = stack.pop();
                        final int target = TTGraphicsState.getCoordsOnVector(gs.projectionVector, x[gs.zp0][gs.rp0], y[gs.zp0][gs.rp0]);
                        final int pMove = target - TTGraphicsState.getCoordsOnVector(gs.projectionVector, x[gs.zp1][p], y[gs.zp1][p]);
                        final int[] shift = gs.getFVMoveforPVDistance(pMove);
                        x[gs.zp1][p] += shift[0];
                        y[gs.zp1][p] += shift[1];

                        final int[] fv = TTGraphicsState.getVectorComponents(gs.freedomVector);
                        if (fv[0] != 0) {
                            touched[gs.zp1][p][0] = true;
                        }
                        if (fv[1] != 0) {
                            touched[gs.zp1][p][1] = true;
                        }
                    }
                    gs.loop = 1;
                    break;

                case RTDG:
                    gs.roundState = TTGraphicsState.dg;
                    gs.gridPeriod = 1.0;
                    break;

                case MIAP0: {
                    final int cvtEntry = stack.pop();
                    final int p = stack.pop();
                    final int target = cvt.get(cvtEntry);
                    final int current = TTGraphicsState.getCoordsOnVector(gs.projectionVector, x[gs.zp0][p], y[gs.zp0][p]);
                    final int pMove = target - current;
                    final int[] shift = gs.getFVMoveforPVDistance(pMove);
                    x[gs.zp0][p] += shift[0];
                    y[gs.zp0][p] += shift[1];

                    final int[] fv = TTGraphicsState.getVectorComponents(gs.freedomVector);
                    if (fv[0] != 0) {
                        touched[gs.zp0][p][0] = true;
                    }
                    if (fv[1] != 0) {
                        touched[gs.zp0][p][1] = true;
                    }

                    gs.rp0 = gs.rp1 = p;
                    break;
                }
                case MIAP1: {
                    final int cvtEntry = stack.pop();
                    final int p = stack.pop();
                    int target = cvt.get(cvtEntry);
                    final int current = TTGraphicsState.getCoordsOnVector(gs.projectionVector, x[gs.zp0][p], y[gs.zp0][p]);

                    //Test if close enough to use cut in value
                    int pMove = target - current;
                    if (Math.abs(pMove) > gs.controlValueTableCutIn) {
                        target = current;
                    }

                    //round
                    target = storeDoubleAsF26Dot6(gs.round(getDoubleFromF26Dot6(target)));

                    //Get and use shift
                    pMove = target - current;
                    final int[] shift = gs.getFVMoveforPVDistance(pMove);
                    x[gs.zp0][p] += shift[0];
                    y[gs.zp0][p] += shift[1];

                    final int[] fv = TTGraphicsState.getVectorComponents(gs.freedomVector);
                    if (fv[0] != 0) {
                        touched[gs.zp0][p][0] = true;
                    }
                    if (fv[1] != 0) {
                        touched[gs.zp0][p][1] = true;
                    }

                    gs.rp0 = gs.rp1 = p;
                    break;
                }
                case NPUSHB:
                    currentPointer++;
                    currentPointer = readFromIS(program[currentPointer], false, currentPointer, program);
                    break;

                case NPUSHW:
                    currentPointer++;
                    currentPointer = readFromIS(program[currentPointer], true, currentPointer, program);
                    break;

                case WS: {
                    final int value = stack.pop();
                    final int key = stack.pop();
                    storage[key] = value;
                    break;
                }
                case RS: {
                    final int key = stack.pop();
                    stack.push(storage[key]);
                    break;
                }
                case WCVTP: {
                    final int value = stack.pop();
                    final int key = stack.pop();
                    cvt.putInPixels(key, value);
                    break;
                }
                case RCVT: {
                    final int key = stack.pop();
                    stack.push(cvt.get(key));
                    break;
                }
                case GC0: {
                    final int p = stack.pop();
                    stack.push(TTGraphicsState.getCoordsOnVector(gs.projectionVector, x[gs.zp2][p], y[gs.zp2][p]));
                    break;
                }
                case GC1: {
                    final int p = stack.pop();
                    stack.push(TTGraphicsState.getCoordsOnVector(gs.dualProjectionVector, x[ORIGINAL + gs.zp2][p], y[ORIGINAL + gs.zp2][p]));
                    break;
                }
                case SCFS: {
                    final int value = stack.pop();
                    final int p = stack.pop();
                    final int current = TTGraphicsState.getCoordsOnVector(gs.projectionVector, x[gs.zp2][p], y[gs.zp2][p]);
                    final int pMove = value - current;
                    final int[] shift = gs.getFVMoveforPVDistance(pMove);
                    x[gs.zp2][p] += shift[0];
                    y[gs.zp2][p] += shift[1];

                    final int[] fv = TTGraphicsState.getVectorComponents(gs.freedomVector);
                    if (fv[0] != 0) {
                        touched[gs.zp2][p][0] = true;
                    }
                    if (fv[1] != 0) {
                        touched[gs.zp2][p][1] = true;
                    }
                    break;
                }
                case MD0: {
                    final int p1 = stack.pop();
                    final int p2 = stack.pop();
                    final int distance = TTGraphicsState.getCoordsOnVector(gs.projectionVector, x[gs.zp1][p2], y[gs.zp1][p2]) -
                            TTGraphicsState.getCoordsOnVector(gs.projectionVector, x[gs.zp0][p1], y[gs.zp0][p1]);
                    stack.push(distance);
                    break;
                }
                case MD1: {
                    final int p1 = stack.pop();
                    final int p2 = stack.pop();
                    final int distance = TTGraphicsState.getCoordsOnVector(gs.dualProjectionVector, x[ORIGINAL + gs.zp1][p2], y[ORIGINAL + gs.zp1][p2]) -
                            TTGraphicsState.getCoordsOnVector(gs.dualProjectionVector, x[ORIGINAL + gs.zp0][p1], y[ORIGINAL + gs.zp0][p1]);
                    stack.push(distance);
                    break;
                }
                case MPPEM: {
                    int pvppem = TTGraphicsState.getCoordsOnVector(gs.projectionVector, (int) (ppem * 64), (int) (ppem * 64)) / 64;
                    if (pvppem < 0) {
                        pvppem = -pvppem;
                    }
                    stack.push(pvppem);
                    break;
                }
                case MPS: {
                    stack.push((int) (ptSize * 64));
                    break;
                }
                case FLIPON:
                    gs.autoFlip = true;
                    break;

                case FLIPOFF:
                    gs.autoFlip = false;
                    break;

                case DEBUG:
                    //shouldn't be in a live font - pops a number
                    stack.pop();
                    break;

                case LT: {
                    final int right = stack.pop();
                    final int left = stack.pop();

                    if (left < right) {
                        stack.push(1);
                    } else {
                        stack.push(0);
                    }
                    break;
                }
                case LTEQ: {
                    final int right = stack.pop();
                    final int left = stack.pop();

                    if (left <= right) {
                        stack.push(1);
                    } else {
                        stack.push(0);
                    }
                    break;
                }
                case GT: {
                    final int right = stack.pop();
                    final int left = stack.pop();

                    if (left > right) {
                        stack.push(1);
                    } else {
                        stack.push(0);
                    }
                    break;
                }
                case GTEQ: {
                    final int right = stack.pop();
                    final int left = stack.pop();

                    if (left >= right) {
                        stack.push(1);
                    } else {
                        stack.push(0);
                    }
                    break;
                }
                case EQ: {
                    final int right = stack.pop();
                    final int left = stack.pop();
                    if (left == right) {
                        stack.push(1);
                    } else {
                        stack.push(0);
                    }
                    break;
                }
                case NEQ: {
                    final int right = stack.pop();
                    final int left = stack.pop();
                    if (left != right) {
                        stack.push(1);
                    } else {
                        stack.push(0);
                    }
                    break;
                }
                case ODD: {
                    int value = stack.pop();
                    value = storeDoubleAsF26Dot6(gs.round(getDoubleFromF26Dot6(value)));
                    value = (value >> 6) % 2;     //remove fractional part and test if odd
                    stack.push(value);
                    break;
                }
                case EVEN: {
                    int value = stack.pop();
                    value = storeDoubleAsF26Dot6(gs.round(getDoubleFromF26Dot6(value)));
                    value = ((value >> 6) + 1) % 2;   //remove fractional part , add 1 and test if odd
                    stack.push(value);
                    break;
                }
                case IF: {
                    //continue or move forward to else/endif
                    final boolean value = stack.pop() != 0;
                    if (!value) {
                        int curr = 0;
                        int nest = 0;
                        do {
                            //deal with nested IF's
                            if (curr == EIF && nest != 0) {
                                nest--;
                            }

                            currentPointer++;
                            curr = program[currentPointer];

                            //deal with nested IF's
                            if (curr == IF) {
                                nest++;
                            }

                            //skip over any data in stream
                            if (curr == NPUSHB) {
                                currentPointer++;
                                currentPointer += program[currentPointer];
                            } else if (curr == NPUSHW) {
                                currentPointer++;
                                currentPointer += program[currentPointer] * 2;
                            } else if (curr >= PUSHB && curr <= PUSHB+7) {
                                currentPointer += (curr + 1) - PUSHB;
                            } else if (curr >= PUSHW && curr <= PUSHW+7) {
                                currentPointer += ((curr + 1) - PUSHW) * 2;
                            }
                        } while ((curr != ELSE && curr != EIF) || nest != 0);
                    }
                    break;
                }
                case EIF:
                    //no implementation necessary
                    break;

                case AND: {
                    final boolean right = stack.pop() != 0;
                    final boolean left = stack.pop() != 0;
                    if (left && right) {
                        stack.push(1);
                    } else {
                        stack.push(0);
                    }
                    break;
                }
                case OR: {
                    final boolean right = stack.pop() != 0;
                    final boolean left = stack.pop() != 0;
                    if (left || right) {
                        stack.push(1);
                    } else {
                        stack.push(0);
                    }
                    break;
                }
                case NOT: {
                    final boolean value = stack.pop() != 0;
                    if (!value) {
                        stack.push(1);
                    } else {
                        stack.push(0);
                    }
                    break;
                }
                case DELTAP1: {
                    final int loop = stack.pop();
                    for (int i = 0; i < loop; i++) {
                        final int p = stack.pop();
                        final int arg = stack.pop();

                        //test ppm
                        final int ppem = gs.deltaBase + (arg >> 4);
                        if (ppem == this.ppem) {

                            //get move
                            int mag = (arg & 0xF) - 7;
                            if (mag <= 0) {
                                mag -= 1;
                            }
                            final int pMove = storeDoubleAsF26Dot6(mag * (1 / Math.pow(2, gs.deltaShift)));

                            //move point
                            final int[] shift = gs.getFVMoveforPVDistance(pMove);
                            x[gs.zp0][p] += shift[0];
                            y[gs.zp0][p] += shift[1];

                            final int[] fv = TTGraphicsState.getVectorComponents(gs.freedomVector);
                            if (fv[0] != 0) {
                                touched[gs.zp0][p][0] = true;
                            }
                            if (fv[1] != 0) {
                                touched[gs.zp0][p][1] = true;
                            }
                        }
                    }
                    break;
                }
                case SDB:
                    gs.deltaBase = stack.pop();
                    break;

                case SDS:
                    gs.deltaShift = stack.pop();
                    break;

                case ADD:
                    stack.push(stack.pop() + stack.pop());
                    break;

                case SUB: {
                    final int right = stack.pop();
                    final int left = stack.pop();
                    stack.push(left - right);
                    break;
                }
                case DIV: {
                    final int right = stack.pop();
                    final int left = stack.pop();
                    if (right != 0) {
                        stack.push((left * 64) / right);
                    } else {
                        stack.push(0);
                    }
                    break;
                }
                case MUL: {
                    final int a = stack.pop();
                    final int b = stack.pop();
                    stack.push(a * b / 64);
                    break;
                }
                case ABS: {
                    int value = stack.pop();
                    if (value < 0) {
                        value = -value;
                    }
                    stack.push(value);
                    break;
                }
                case NEG:
                    stack.push(-stack.pop());
                    break;

                case FLOOR:
                    stack.push((stack.pop() >> 6) << 6);
                    break;

                case CEILING: {
                    int value = stack.pop();
                    if ((value & 63) != 0) {
                        value = (((value >> 6) + 1) << 6);
                    }
                    stack.push(value);
                    break;
                }
                case ROUND00: {
                    int n = stack.pop();
                    n = engineCompensation(n, 0);
                    final double num = getDoubleFromF26Dot6(n);
                    stack.push(storeDoubleAsF26Dot6(gs.round(num)));
                    break;
                }
                case ROUND01: {
                    int n = stack.pop();
                    n = engineCompensation(n, 1);
                    final double num = getDoubleFromF26Dot6(n);
                    stack.push(storeDoubleAsF26Dot6(gs.round(num)));
                    break;
                }
                case ROUND10: {
                    int n = stack.pop();
                    n = engineCompensation(n, 2);
                    final double num = getDoubleFromF26Dot6(n);
                    stack.push(storeDoubleAsF26Dot6(gs.round(num)));
                    break;
                }
                case ROUND11: {
                    int n = stack.pop();
                    n = engineCompensation(n, 3);
                    final double num = getDoubleFromF26Dot6(n);
                    stack.push(storeDoubleAsF26Dot6(gs.round(num)));
                    break;
                }
                case NROUND00:
                    stack.push(engineCompensation(stack.pop(), 0));
                    break;

                case NROUND01:
                    stack.push(engineCompensation(stack.pop(), 1));
                    break;

                case NROUND10:
                    stack.push(engineCompensation(stack.pop(), 2));
                    break;

                case NROUND11:
                    stack.push(engineCompensation(stack.pop(), 3));
                    break;

                case WCVTF: {
                    final int value = stack.pop();
                    final int key = stack.pop();
                    cvt.putInFUnits(key, value);
                    break;
                }
                case DELTAP2: {
                    final int loop = stack.pop();
                    for (int i = 0; i < loop; i++) {
                        final int p = stack.pop();
                        final int arg = stack.pop();

                        //test ppm
                        final int ppem = gs.deltaBase + 16 + (arg >> 4);
                        if (ppem == this.ppem) {

                            //get move
                            int mag = (arg & 0xF) - 7;
                            if (mag <= 0) {
                                mag -= 1;
                            }
                            final int pMove = storeDoubleAsF26Dot6(mag * (1 / Math.pow(2, gs.deltaShift)));

                            //move point
                            final int[] shift = gs.getFVMoveforPVDistance(pMove);
                            x[gs.zp0][p] += shift[0];
                            y[gs.zp0][p] += shift[1];

                            final int[] fv = TTGraphicsState.getVectorComponents(gs.freedomVector);
                            if (fv[0] != 0) {
                                touched[gs.zp0][p][0] = true;
                            }
                            if (fv[1] != 0) {
                                touched[gs.zp0][p][1] = true;
                            }
                        }
                    }
                    break;
                }
                case DELTAP3: {
                    final int loop = stack.pop();
                    for (int i = 0; i < loop; i++) {
                        final int p = stack.pop();
                        final int arg = stack.pop();

                        //test ppm
                        final int ppem = gs.deltaBase + 32 + (arg >> 4);
                        if (ppem == this.ppem) {

                            //get move
                            int mag = (arg & 0xF) - 7;
                            if (mag <= 0) {
                                mag -= 1;
                            }
                            final int pMove = storeDoubleAsF26Dot6(mag * (1 / Math.pow(2, gs.deltaShift)));

                            //move point
                            final int[] shift = gs.getFVMoveforPVDistance(pMove);
                            x[gs.zp0][p] += shift[0];
                            y[gs.zp0][p] += shift[1];

                            final int[] fv = TTGraphicsState.getVectorComponents(gs.freedomVector);
                            if (fv[0] != 0) {
                                touched[gs.zp0][p][0] = true;
                            }
                            if (fv[1] != 0) {
                                touched[gs.zp0][p][1] = true;
                            }
                        }
                    }
                    break;
                }
                case DELTAC1: {
                    final int loop = stack.pop();
                    for (int i = 0; i < loop; i++) {
                        final int cvtEntry = stack.pop();
                        final int arg = stack.pop();

                        //test ppm
                        final int ppem = gs.deltaBase + (arg >> 4);
                        if (ppem == this.ppem) {

                            //get change
                            int mag = (arg & 0xF) - 7;
                            if (mag <= 0) {
                                mag -= 1;
                            }
                            final int change = storeDoubleAsF26Dot6(mag * (1 / Math.pow(2, gs.deltaShift)));

                            //change value
                            int value = cvt.get(cvtEntry);
                            value += change;
                            cvt.putInPixels(cvtEntry, value);
                        }
                    }
                    break;
                }
                case DELTAC2: {
                    final int loop = stack.pop();
                    for (int i = 0; i < loop; i++) {
                        final int cvtEntry = stack.pop();
                        final int arg = stack.pop();

                        //test ppm
                        final int ppem = gs.deltaBase + 16 + (arg >> 4);
                        if (ppem == this.ppem) {

                            //get change
                            int mag = (arg & 0xF) - 7;
                            if (mag <= 0) {
                                mag -= 1;
                            }
                            final int change = storeDoubleAsF26Dot6(mag * (1 / Math.pow(2, gs.deltaShift)));

                            //change value
                            int value = cvt.get(cvtEntry);
                            value += change;
                            cvt.putInPixels(cvtEntry, value);
                        }
                    }
                    break;
                }
                case DELTAC3: {
                    final int loop = stack.pop();
                    for (int i = 0; i < loop; i++) {
                        final int cvtEntry = stack.pop();
                        final int arg = stack.pop();

                        //test ppm
                        final int ppem = gs.deltaBase + 32 + (arg >> 4);
                        if (ppem == this.ppem) {

                            //get change
                            int mag = (arg & 0xF) - 7;
                            if (mag <= 0) {
                                mag -= 1;
                            }
                            final int change = storeDoubleAsF26Dot6(mag * (1 / Math.pow(2, gs.deltaShift)));

                            //change value
                            int value = cvt.get(cvtEntry);
                            value += change;
                            cvt.putInPixels(cvtEntry, value);
                        }
                    }
                    break;
                }
                case SROUND:
                    gs.roundState = stack.pop();
                    gs.gridPeriod = 1.0;
                    break;

                case S45ROUND:
                    gs.roundState = stack.pop();
                    gs.gridPeriod = 0.7071067811865476;  //Math.sqrt(2)/2
                    break;

                case JROT: {
                    final boolean jump = stack.pop() != 0;
                    final int amount = stack.pop();
                    if (jump) {
                        currentPointer = currentPointer + amount - 1;
                    }
                    break;
                }
                case JROF: {
                    final boolean jump = stack.pop() != 0;
                    final int amount = stack.pop();
                    if (!jump) {
                        currentPointer = currentPointer + amount - 1;
                    }
                    break;
                }
                case ROFF:
                    gs.roundState = TTGraphicsState.off;
                    break;

                case RUTG:
                    gs.roundState = TTGraphicsState.utg;
                    gs.gridPeriod = 1.0;
                    break;

                case RDTG:
                    gs.roundState = TTGraphicsState.dtg;
                    gs.gridPeriod = 1.0;
                    break;

                case SANGW:
                    //Deprecated method - now only pops a value from stack
                    stack.pop();
                    break;

                case AA:
                    //Deprecated method - now only pops a value from stack
                    stack.pop();
                    break;

                case FLIPPT: {
                    for (int i = 0; i < gs.loop; i++) {
                        final int point = stack.pop();
                        curve[gs.zp0][point] = !curve[gs.zp0][point];
                    }
                    gs.loop = 1;
                    break;
                }
                case FLIPRGON: {
                    final int high = stack.pop();
                    final int low = stack.pop();
                    for (int i = low; i <= high; i++) {
                        curve[gs.zp0][i] = true;
                    }
                    break;
                }
                case FLIPRGOFF: {
                    final int high = stack.pop();
                    final int low = stack.pop();
                    for (int i = low; i <= high; i++) {
                        curve[gs.zp0][i] = false;
                    }
                    break;
                }
                case SCANCTRL:
                    stack.pop();
                    break;

                case SDPVTL0: {
                    final int p2 = stack.pop();
                    final int p1 = stack.pop();
                    double xdiff = getDoubleFromF26Dot6(x[gs.zp2][p2] - x[gs.zp1][p1]);
                    double ydiff = getDoubleFromF26Dot6(y[gs.zp2][p2] - y[gs.zp1][p1]);
                    double dxdiff = getDoubleFromF26Dot6(x[ORIGINAL + gs.zp2][p2] - x[ORIGINAL + gs.zp1][p1]);
                    double dydiff = getDoubleFromF26Dot6(y[ORIGINAL + gs.zp2][p2] - y[ORIGINAL + gs.zp1][p1]);
                    final double factor = Math.sqrt((xdiff * xdiff) + (ydiff * ydiff));
                    final double dfactor = Math.sqrt((dxdiff * dxdiff) + (dydiff * dydiff));
                    xdiff /= factor;
                    ydiff /= factor;
                    dxdiff /= dfactor;
                    dydiff /= dfactor;
                    gs.projectionVector = TTGraphicsState.createVector(storeDoubleAsF2Dot14(xdiff), storeDoubleAsF2Dot14(ydiff));
                    gs.dualProjectionVector = TTGraphicsState.createVector(storeDoubleAsF2Dot14(dxdiff), storeDoubleAsF2Dot14(dydiff));
                    break;
                }
                case SDPVTL1: {
                    final int p2 = stack.pop();
                    final int p1 = stack.pop();
                    double xdiff = getDoubleFromF26Dot6(x[gs.zp2][p2] - x[gs.zp1][p1]);
                    double ydiff = getDoubleFromF26Dot6(y[gs.zp2][p2] - y[gs.zp1][p1]);
                    double dxdiff = getDoubleFromF26Dot6(x[ORIGINAL + gs.zp2][p2] - x[ORIGINAL + gs.zp1][p1]);
                    double dydiff = getDoubleFromF26Dot6(y[ORIGINAL + gs.zp2][p2] - y[ORIGINAL + gs.zp1][p1]);
                    final double factor = Math.sqrt((xdiff * xdiff) + (ydiff * ydiff));
                    final double dfactor = Math.sqrt((dxdiff * dxdiff) + (dydiff * dydiff));
                    xdiff /= factor;
                    ydiff /= factor;
                    dxdiff /= dfactor;
                    dydiff /= dfactor;
                    gs.projectionVector = TTGraphicsState.createVector(storeDoubleAsF2Dot14(ydiff), storeDoubleAsF2Dot14(-xdiff));
                    gs.dualProjectionVector = TTGraphicsState.createVector(storeDoubleAsF2Dot14(dydiff), storeDoubleAsF2Dot14(-dxdiff));
                    break;
                }
                case GETINFO: {
                    final int selector = stack.pop();
                    int result = 0;

                    if ((selector & 1) == 1) {
                        result += 3;
                    }

//                    //Currently not needed as we don't use isRotated or isStretched
//                    if ((selector & 2) == 2 && isRotated)
//                        result += 0x100;
//
//                    if ((selector & 4) == 4 && isStretched)
//                        result += 0x200;

                    stack.push(result);
                    break;
                }
                case IDEF: {
                    final int func = stack.pop();
                    final int start = currentPointer;

                    //work out length
                    int curr;
                    do {
                        currentPointer++;
                        curr = program[currentPointer];
                    } while (curr != ENDF);
                    final int len = (currentPointer - start) - 1;
                    currentPointer = start;

                    //create function
                    final int[] instruction = new int[len];
                    for (int i = 0; i < len; i++) {
                        currentPointer++;
                        instruction[i] = program[currentPointer];
                    }
                    instructions.put(func, instruction);

                    //skip past ENDF
                    currentPointer++;

                    break;
                }
                case ROLL: {
                    final int top = stack.pop();
                    final int middle = stack.pop();
                    final int bottom = stack.pop();
                    stack.push(middle);
                    stack.push(top);
                    stack.push(bottom);
                    break;
                }
                case MAX: {
                    final int value1 = stack.pop();
                    final int value2 = stack.pop();

                    if (value1 > value2) {
                        stack.push(value1);
                    } else {
                        stack.push(value2);
                    }
                    break;
                }
                case MIN: {
                    final int value1 = stack.pop();
                    final int value2 = stack.pop();

                    if (value1 < value2) {
                        stack.push(value1);
                    } else {
                        stack.push(value2);
                    }
                    break;
                }
                case SCANTYPE:
                    stack.pop();
                    break;

                case INSTCTRL: {
                    final int s = stack.pop();
                    final int value = stack.pop();
                    if (s == 1) {
                        gs.instructControl = value;
                    } else if (s == 2) {
                        useDefaultGS = value == 2;
                    }
                    break;
                }
                case PUSHB:
                    if (printOut) {
                        System.out.println("PUSHB1    - Push bytes from IS to stack");
                    }
                    currentPointer = readFromIS(bytesToRead, false, currentPointer, program);
                    break;

                case PUSHW:
                    if (printOut) {
                        System.out.println("PUSHW1    - Push words from IS to stack");
                    }
                    currentPointer = readFromIS(bytesToRead, true, currentPointer, program);
                    break;

                default:

                    if (code >= MDRP && code < MDRP + 0x20) {
                        final int args = code - MDRP;

                        if (printOut) {
                            System.out.println("MDRP      - Move direct relative point (" + Integer.toBinaryString(args) + ')');
                        }

                        //read args
                        boolean setRP0toP = false, useMinimumDistance = false, roundDistance = false;
                        if ((args & paramRESETRP0) == paramRESETRP0) {
                            setRP0toP = true;
                        }
                        if ((args & paramUSEMINDIST) == paramUSEMINDIST) {
                            useMinimumDistance = true;
                        }
                        if ((args & paramROUND) == paramROUND) {
                            roundDistance = true;
                        }
                        final int distanceType = args & 3;

                        final int p = stack.pop();

                        //get original distance
                        int originalDistance = TTGraphicsState.getCoordsOnVector(gs.dualProjectionVector, x[ORIGINAL + gs.zp1][p], y[ORIGINAL + gs.zp1][p]) -
                                TTGraphicsState.getCoordsOnVector(gs.dualProjectionVector, x[ORIGINAL + gs.zp0][gs.rp0], y[ORIGINAL + gs.zp0][gs.rp0]);

                        //check single width cut in
                        if (Math.abs(originalDistance) < gs.singleWidthCutIn) {
                            if (originalDistance > 0) {
                                originalDistance = gs.singleWidthValue;
                            } else {
                                originalDistance = -gs.singleWidthValue;
                            }
                        }

                        //compensate for engine characteristics
                        originalDistance = engineCompensation(originalDistance, distanceType);

                        //round
                        if (roundDistance) {
                            originalDistance = storeDoubleAsF26Dot6(gs.round(getDoubleFromF26Dot6(originalDistance)));
                        }

                        //use minimum distance
                        if (useMinimumDistance && Math.abs(originalDistance) < gs.minimumDistance) {
                            if (originalDistance < 0) {
                                originalDistance = -gs.minimumDistance;
                            } else {
                                originalDistance = gs.minimumDistance;
                            }
                        }

                        //Get move needed
                        final int target = TTGraphicsState.getCoordsOnVector(gs.projectionVector, x[gs.zp0][gs.rp0], y[gs.zp0][gs.rp0]) + originalDistance;
                        final int pVMove = target - TTGraphicsState.getCoordsOnVector(gs.projectionVector, x[gs.zp1][p], y[gs.zp1][p]);

                        //move point
                        final int[] shift = gs.getFVMoveforPVDistance(pVMove);
                        x[gs.zp1][p] += shift[0];
                        y[gs.zp1][p] += shift[1];

                        //mark as touched
                        final int[] fv = TTGraphicsState.getVectorComponents(gs.freedomVector);
                        if (fv[0] != 0) {
                            touched[gs.zp1][p][0] = true;
                        }
                        if (fv[1] != 0) {
                            touched[gs.zp1][p][1] = true;
                        }

                        //inexplicable reference point settings
                        gs.rp1 = gs.rp0;
                        gs.rp2 = p;
                        if (setRP0toP) {
                            gs.rp0 = p;
                        }

                    } else if (code >= MIRP && code <= MIRP + 0x1F) {
                        final int args = code - MIRP;
                        if (printOut) {
                            System.out.println("MIRP      - Move Indirect Relative Point(" + Integer.toBinaryString(args) + ')');
                        }

                        //read args
                        boolean setRP0toP = false, useMinimumDistance = false, roundDistanceAndCheckCutIn = false;
                        if ((args & paramRESETRP0) == paramRESETRP0) {
                            setRP0toP = true;
                        }
                        if ((args & paramUSEMINDIST) == paramUSEMINDIST) {
                            useMinimumDistance = true;
                        }
                        if ((args & paramROUND) == paramROUND) {
                            roundDistanceAndCheckCutIn = true;
                        }
                        final int distanceType = args & 3;

                        int cvtEntry = cvt.get(stack.pop());
                        final int p = stack.pop();

                        //Get original distance
                        int distance = TTGraphicsState.getCoordsOnVector(gs.dualProjectionVector, x[ORIGINAL + gs.zp1][p], y[ORIGINAL + gs.zp1][p]) -
                                TTGraphicsState.getCoordsOnVector(gs.dualProjectionVector, x[ORIGINAL + gs.zp0][gs.rp0], y[ORIGINAL + gs.zp0][gs.rp0]);

                        //Check single width cutin
                        if (Math.abs(distance - gs.singleWidthValue) < gs.singleWidthCutIn) {
                            distance = gs.singleWidthValue;
                        }

                        //Check CVT cut-in
                        if (roundDistanceAndCheckCutIn) {
                            //Check autoflip and match CVT sign to distance sign
                            if (gs.autoFlip && ((distance < 0 && cvtEntry > 0) || (distance > 0 && cvtEntry < 0))) {
                                cvtEntry = -cvtEntry;
                            }

                            if (Math.abs(distance - cvtEntry) < gs.controlValueTableCutIn) {
                                distance = cvtEntry;
                            }
                        }

                        //Compensate for engine characteristics
                        distance = engineCompensation(distance, distanceType);

                        //Round
                        if (roundDistanceAndCheckCutIn) {
                            distance = gs.round(distance);
                        }

                        //Check minimum distance
                        if (useMinimumDistance && Math.abs(distance) < gs.minimumDistance) {
                            if (distance > 0) {
                                distance = gs.minimumDistance;
                            } else {
                                distance = -gs.minimumDistance;
                            }
                        }

                        //Get move needed
                        final int target = TTGraphicsState.getCoordsOnVector(gs.projectionVector, x[gs.zp0][gs.rp0], y[gs.zp0][gs.rp0]) + distance;
                        final int pVMove = target - TTGraphicsState.getCoordsOnVector(gs.projectionVector, x[gs.zp1][p], y[gs.zp1][p]);
                        final int[] shift = gs.getFVMoveforPVDistance(pVMove);

                        //Perform shift
                        x[gs.zp1][p] += shift[0];
                        y[gs.zp1][p] += shift[1];

                        //mark as touched
                        final int[] fv = TTGraphicsState.getVectorComponents(gs.freedomVector);
                        if (fv[0] != 0) {
                            touched[gs.zp1][p][0] = true;
                        }
                        if (fv[1] != 0) {
                            touched[gs.zp1][p][1] = true;
                        }

                        //inexplicable reference point settings
                        gs.rp1 = gs.rp0;
                        gs.rp2 = p;
                        if (setRP0toP) {
                            gs.rp0 = p;
                        }

                    } else if (instructions.containsKey(Integer.valueOf(code))) {
                        if (printOut) {
                            System.out.println("I 0x" + Integer.toHexString(code) + "    - Custom Instruction");
                        }
                        execute(instructions.get(Integer.valueOf(code)), gs);
                        if (printOut) {
                            System.out.println("I 0x" + Integer.toHexString(code) + " finished");
                        }

                    } else if(LogWriter.isRunningFromIDE){
                        System.out.println("Unknown truetype opcode 0x" + Integer.toHexString(code) + " at line " + currentPointer);
                    }
            }
        } catch(final Exception e) {

            LogWriter.writeLog("Exception: " + e.getMessage()+ " at line "+currentPointer+"- hinting turned off");
            
            BaseTTGlyph.useHinting = false;
            BaseTTGlyph.redecodePage = true;
        }

        if (showDebugWindow && debugWindow != null && debugWindow.isVisible()) {
            instructionsExecuted += currentPointer + 1 - originalPointer;

            if (debugPointer==-1) {
                return debugPointer;
            }
        }
        
        return currentPointer;
    }


    /**
     * Goes through the glyph contour by contour finding pairs of touched points and moving the points between them to
     * preserve the shape of the original outline.
     *
     * @param direction Whether to interpolate in the x or y axis (value is originating instruction)
     */
    private void interpolateUntouchedPoints(final int direction) {

        //Set values according to direction
        final int[] points;
        final int[] original;
        final boolean[] touched = new boolean[this.touched[GLYPH_ZONE].length];

        if (direction == IUPx) {
            points = x[GLYPH_ZONE];
            original = x[ORIGINAL+GLYPH_ZONE];
            for (int i =0; i<this.touched[GLYPH_ZONE].length; i++) {
                touched[i] = this.touched[GLYPH_ZONE][i][0];
            }
        } else {
            points = y[GLYPH_ZONE];
            original = y[ORIGINAL+GLYPH_ZONE];
            for (int i =0; i<this.touched[GLYPH_ZONE].length; i++) {
                touched[i] = this.touched[GLYPH_ZONE][i][1];
            }
        }

        //go through contours
        int contourStart=0;
        while (contourStart < points.length) {

            //get info on touched points
            final int[] touchedPointNumbers = new int[original.length];
            int touchedCount=0, point=0;
            do {
                if (touched[contourStart+point]) {
                    touchedPointNumbers[touchedCount] = contourStart+point;
                    touchedCount++;
                }
                point++;
            } while(!contour[GLYPH_ZONE][contourStart+point-1] && (contourStart+point) < contour[GLYPH_ZONE].length);


            //process points
            if (touchedCount == 1) {

                //If only one touched point, shift all points in contour to match
                final int shift = points[touchedPointNumbers[0]] - original[touchedPointNumbers[0]];
                for (int i=contourStart; i<contourStart+point; i++) {
                    if (!touched[i]) {
                        points[i] += shift;
                    }
                }
            } else if (touchedCount > 1) {

                //Loop through pairs interpolating the points between them
                for (int i=0; i<touchedCount; i++) {
                    if (i+1 >= touchedCount) {
                        //Special case for between the last and first touched points
                        interpolateRange(touchedPointNumbers[i]+1,   contourStart+point-1,       touchedPointNumbers[i],     touchedPointNumbers[0],     points,original);
                        interpolateRange(contourStart,               touchedPointNumbers[0]-1,   touchedPointNumbers[i],     touchedPointNumbers[0],     points,original);
                    } else {
                        interpolateRange(touchedPointNumbers[i]+1,   touchedPointNumbers[i+1]-1, touchedPointNumbers[i],     touchedPointNumbers[i+1],   points,original);
                    }
                }
            }


            //Move to start of next contour
            contourStart += point;
        }
    }

    /**
     * Interpolates the values of a range of points using two reference points. If the points coordinates were
     * originally between those of the two reference points, the relationship is maintained. If not, it is shifted by
     * the same shift which has been applied to the nearest of the two reference points.
     *
     * @param start The first point to be interpolated
     * @param end The last point to be interpolated
     * @param ref1 The first reference point
     * @param ref2 The second reference point
     * @param points The current coordinates of all points
     * @param original The original coordinates of all points
     */
    private static void interpolateRange(final int start, final int end, final int ref1, final int ref2, final int[] points, final int[] original) {

        //Work out which reference point is higher/lower
        final int lowerRef;
        final int higherRef;
        if (original[ref2] < original[ref1]) {
            lowerRef = ref2;
            higherRef = ref1;
        } else {
            lowerRef = ref1;
            higherRef = ref2;
        }

        //Go through points
        for (int i=start; i<=end; i++) {

            //If below/left of both reference points shift by the bottom/left point
            if (original[i] < original[lowerRef]) {
                points[i] += (points[lowerRef] - original[lowerRef]);

                //If above/right of both reference points shift by the top/right point
            } else if (original[i] > original[higherRef]) {
                points[i] += (points[higherRef] - original[higherRef]);

                //If between the reference points interpolate the new value
            } else {
                final double pos = (double)(original[i] - original[lowerRef])/(original[higherRef] - original[lowerRef]);
                points[i] = points[lowerRef] + (int)(pos * (points[higherRef] - points[lowerRef]));
            }
        }
    }



    /**
     * Doesn't currently do anything - should compensate for large dot sizes on some printers
     * @param num Number to compensate
     * @param characteristics Type of compensation to use
     * @return Compensated number
     */
    @SuppressWarnings("UnusedParameters")
    private static int engineCompensation(final int num, final int characteristics) {
        return num;
    }

    /**
     * Reads data from the Input Stream and puts it on the stack
     * @param number How many items to read
     * @param readWord Whether you're reading a word or a byte
     * @param currentPointer The current location in the stream
     * @param program The current input stream
     * @return The final location in the stream
     */
    private int readFromIS(final int number, final boolean readWord, int currentPointer, final int[] program) {
        for (int i=0; i<number; i++) {
            final int data;

            currentPointer++;
            if (!readWord) {
                data = program[currentPointer];
            } else {
                final int d1 = program[currentPointer];
                currentPointer++;
                final int d2 = program[currentPointer];
                data = getIntFrom2Uint8(d1, d2);
            }

            //push to stack
            stack.push(data);
        }
        return currentPointer;
    }


    /**
     * Takes two Uint8s containing 8 bits of data each and converts them to
     * a signed integer.
     * @param high first int
     * @param low second int
     * @return signed int
     */
    protected static int getIntFrom2Uint8(final int high, final int low) {
        return ((high << 8) + low) +        //main concatenation
                ((high >> 7 & 1)*-65536);   //account for negative option
    }

    /**
     * Takes a F26Dot6 number and returns the value as a double.
     * @param a F26Dot6 value
     * @return Double value
     */
    protected static double getDoubleFromF26Dot6(final int a) {
        return (double)a/64;
    }

    /**
     * Takes a F2Dot14 number and returns the value as a double.
     * @param a F2Dot14 value
     * @return Double value
     */
    protected static double getDoubleFromF2Dot14(final int a) {
        return (double)a/0x4000;
    }

    /**
     * Takes a double and returns the value as a F26Dot6 number.
     * @param a Double value
     * @return F26Dot6 value
     */
    protected static int storeDoubleAsF26Dot6(final double a) {
        return (int)((a*64)+0.5);
    }

    /**
     * Takes a double and returns the value as a F2Dot14 number.
     * @param a Double value
     * @return F2Dot14 value
     */
    protected static int storeDoubleAsF2Dot14(final double a) {
        return (int)((a*16384)+0.5);
    }


    /**
     * Reads a program from a table in the font file.
     * @param currentFontFile Font file to use
     * @param table Table ID
     * @return The program
     */
    private static int[] readProgramTable(final FontFile2 currentFontFile, final int table) {
        int[] program = {};

        //move to start and check exists
        final int startPointer=currentFontFile.selectTable(table);

        //read table
        if(startPointer==0){
            LogWriter.writeLog("No program table found: " + table);
        }else{
            final int len = currentFontFile.getOffset(table);
            program = new int[len];
            for (int i = 0; i < len; i++) {
                program[i] =currentFontFile.getNextUint8();
            }
        }

        return program;
    }


    /**
     * Stack used by programs
     */
    private static class Stack implements Serializable {
        private int pointer;
        private int[] stack;

        Stack() {
            stack = new int[10];
        }

        /**
         * Adds an item to the top of the stack, expanding the stack if needed
         * @param a New item for stack
         */
        public void push(final int a) {
            if (pointer >= stack.length) {
                final int[] newStack = new int[(int)(stack.length*1.5)];
                System.arraycopy(stack,0,newStack,0,stack.length);
                stack = newStack;

            }
            stack[pointer] = a;

            pointer++;
        }

        /**
         * Removes an item from the top of the stack
         * @return removed item
         */
        public int pop() {
            pointer--;
            if (pointer >= 0) {
                return stack[pointer];
            }

            throw new RuntimeException("Popped an empty stack!");
        }

        /**
         * @return The number of items on the stack
         */
        public int size() {
            return pointer;
        }

        /**
         * Accesses an element further down the stack
         * @param key The number (from the top down) of the item to access
         * @return  The item
         */
        public int elementAt(final int key) {
            return stack[pointer-key];
        }

        /**
         * Removes an item from the stack
         * @param key The number (from the top down) of the item to remove
         * @return The removed item
         */
        public int remove(final int key) {
            final int valPos = pointer-key;
            final int result = stack[valPos];
            final int[] newStack = new int[stack.length];
            System.arraycopy(stack, 0, newStack, 0, valPos);
            System.arraycopy(stack, valPos+1, newStack, valPos, (stack.length-valPos)-1);
            stack = newStack;
            pointer--;
            return result;
        }
        
        /**
         * DEBUG METHOD -
         *
         * Print out the top 5 elements on the stack
         */
        public void print() {
            System.out.println("stack: ");
            int i;
            for (i=pointer-1; i>=0 && i >=pointer-5; i--) {
                System.out.println(i+": "+stack[i]);
            }
            if (i>0) {
                System.out.println("...");
            }
            System.out.println("");
        }

        public String[] toStringArray() {
            final String[] result = new String[pointer];

            for (int i=pointer-1; i >= 0; i--) {
                result[(pointer - i) - 1] = (pointer - i) - 1 + ": " + stack[i] + "       (" + NumberFormat.getNumberInstance().format(stack[i] / 64d) + ')';
            }

            return result;
        }
    }
    
    /**
     * DEBUG METHOD -
     *
     * Sets up the Hinting Debugger.
     */
    private void runDebugger() {
        if (programToDebug == null) {
            JOptionPane.showMessageDialog(debugWindow, "No glyph program found to debug!");
            return;
        }

        debugWindow = new JFrame("TrueType Hinting Debugger");
        debugWindow.setSize(1000,700);
        debugWindow.setLayout(new BorderLayout());


        /**
         * Top panel
         */
        //Buttons
        final JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

        final JButton stepOverButton = new JButton("Step Over");
        stepOverButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent e) {
                advanceDebugger(false);
            }
        });
        buttonPanel.add(stepOverButton);

        final JButton stepIntoButton = new JButton("Step Into");
        stepIntoButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent e) {
                advanceDebugger(true);
            }
        });
        buttonPanel.add(stepIntoButton);

        final JButton stepOutButton = new JButton("Step Out");
        stepOutButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent e) {
                runDebuggerTo(programToDebug.length);
            }
        });
        buttonPanel.add(stepOutButton);

        final JButton restartButton = new JButton("Restart");
        restartButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent e) {
                restartDebugger();
                refreshDebugger(true);
            }
        });
        buttonPanel.add(restartButton);

        final JButton backButton = new JButton("Back");
        backButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent e) {
                runDebuggerTo(debugPointer - 1);
            }
        });
        buttonPanel.add(backButton);

        debugWindow.add(BorderLayout.NORTH,buttonPanel);

        /**
         * Left panel
         */
        final JPanel instructionPanel = new JPanel();
        instructionPanel.setLayout(new BorderLayout());
        instructionPanel.setBorder(new javax.swing.border.LineBorder(Color.BLACK));

        //Code list
        currentInstructionList = new JList();
        final JScrollPane codePane = new JScrollPane(currentInstructionList) {
            @Override
            public Dimension getPreferredSize() {
                final Dimension pref = super.getPreferredSize();
                final Dimension min = getMinimumSize();

                final int w = pref.width < min.width ? min.width : pref.width;
                final int h = pref.height < min.height ? min.height : pref.height;

                return new Dimension(w, h);
            }
        };
        codePane.setMinimumSize(new Dimension(150,100));

        //Label
        currentCode = new JLabel("Glyph program");

        //Add to panel
        instructionPanel.add(BorderLayout.CENTER,codePane);
        instructionPanel.add(BorderLayout.SOUTH,currentCode);

        debugWindow.add(BorderLayout.WEST,instructionPanel);




        /**
         * Centre panel
         */
        final JPanel glyphPanel = new JPanel();
        glyphPanel.setLayout(new BorderLayout());
        glyphPanel.setBorder(new javax.swing.border.LineBorder(Color.BLACK));

        debugGlyphDisplay = new JComponent() {
            @Override
            public void paint(final Graphics g) {
                final Graphics2D g2 = (Graphics2D)g;

                //Calculate Scale
                final int w = getWidth();
                final int h = getHeight();

                int minX=Integer.MAX_VALUE;
                int minY=Integer.MAX_VALUE;
                int maxX=Integer.MIN_VALUE;
                int maxY=Integer.MIN_VALUE;
                for (int i=0; i<x[GLYPH_ZONE].length; i++) {
                    int val = x[GLYPH_ZONE][i];
                    if (val > maxX) {
                        maxX = val;
                    }
                    if (val < minX) {
                        minX = val;
                    }

                    val = y[GLYPH_ZONE][i];
                    if (val > maxY) {
                        maxY = val;
                    }
                    if (val < minY) {
                        minY = val;
                    }
                }

                int xRange = maxX - minX;
                int yRange = maxY - minY;
                double xScale = (double)w / xRange;
                double yScale = (double)h / yRange;
                double scale = xScale < yScale ? xScale : yScale;

                //add buffer area
                final int borderWidth = 15;
                minX -= (borderWidth/scale);
                maxX += (borderWidth/scale);
                minY -= (borderWidth/scale);
                maxY += (borderWidth/scale);

                //recalculate scale
                xRange = maxX - minX;
                yRange = maxY - minY;
                xScale = (double)w / xRange;
                yScale = (double)h / yRange;
                scale = xScale < yScale ? xScale : yScale;


                //Apply transform
                g2.translate(0, h);
                g2.scale(scale, -scale);

                g2.translate(-minX, -minY);


                //Fill with white
                g2.setPaint(Color.WHITE);
                g2.fillRect(minX,minY,(int)(w/scale),(int)(h/scale));


                //Draw axes
                g2.setPaint(new Color(180,180,255));
                g2.drawLine(0, minY, 0, (int)(h/scale));
                g2.drawLine(minX, 0, (int)(w/scale), 0);


                //Draw points
                final int len = (int)(3 / scale);
                for (int i=0; i<x[GLYPH_ZONE].length; i++) {
                    final int xVal = x[GLYPH_ZONE][i];
                    final int yVal = y[GLYPH_ZONE][i];

                    //Point
                    if (curve[GLYPH_ZONE][i]) {
                        g2.setPaint(Color.BLACK);
                        final Shape s = new Ellipse2D.Double(xVal-(2/scale),yVal-(2/scale),(4/scale),(4/scale));
                        g2.fill(s);
                    } else {
                        g2.setPaint(Color.RED);
                        g2.drawLine(xVal-len, yVal-len, xVal+len, yVal+len);
                        g2.drawLine(xVal+len, yVal-len, xVal-len, yVal+len);
                    }

                    //Number
                    final AffineTransform store = g2.getTransform();
                    g2.translate(xVal, yVal);
                    g2.scale(1/scale,-1/scale);
                    g2.drawString(String.valueOf(i), 3, -3);

                    g2.setTransform(store);
                }


                //Draw interpolated shadow
                if (showInterpolatedShadow.isSelected()) {
                    final int c = x[GLYPH_ZONE].length;
                    final int[] xStore = new int[c];
                    System.arraycopy(x[GLYPH_ZONE], 0, xStore, 0, c);
                    final int[] yStore = new int[c];
                    System.arraycopy(y[GLYPH_ZONE], 0, yStore, 0, c);
                    final boolean[] curveStore = new boolean[c];
                    System.arraycopy(curve[GLYPH_ZONE], 0, curveStore, 0, c);
                    final boolean[] contourStore = new boolean[c];
                    System.arraycopy(contour[GLYPH_ZONE], 0, contourStore, 0, c);
                    interpolateUntouchedPoints(IUPy);
                    interpolateUntouchedPoints(IUPx);
                    final GeneralPath shape = getPathFromPoints(x[GLYPH_ZONE],y[GLYPH_ZONE],curve[GLYPH_ZONE],contour[GLYPH_ZONE]);
                    g2.setPaint(new Color(255,0,0,100));
                    g2.draw(shape);
                    g2.setPaint(new Color(0, 0, 0, 30));
                    g2.fill(shape);
                    System.arraycopy(xStore, 0, x[GLYPH_ZONE], 0, c);
                    System.arraycopy(yStore, 0, y[GLYPH_ZONE], 0, c);
                    System.arraycopy(curveStore, 0, curve[GLYPH_ZONE], 0, c);
                    System.arraycopy(contourStore, 0, contour[GLYPH_ZONE], 0, c);
                }


                //Draw glyph
                final GeneralPath shape = getPathFromPoints(x[GLYPH_ZONE],y[GLYPH_ZONE],curve[GLYPH_ZONE],contour[GLYPH_ZONE]);

                g2.setPaint(new Color(100,100,255,100));
                g2.fill(shape);
                g2.setPaint(Color.BLACK);
                g2.draw(shape);

                //Draw Twilight points
                g2.setPaint(Color.BLUE);
                for (int i=0; i<x[TWILIGHT_ZONE].length; i++) {
                    final int xVal = x[TWILIGHT_ZONE][i];
                    final int yVal = y[TWILIGHT_ZONE][i];

                    if (xVal != 0 || yVal != 0) {
                        //Point
                        if (curve[TWILIGHT_ZONE][i]) {
                            final Shape s = new Ellipse2D.Double(xVal-(2/scale),yVal-(2/scale),(4/scale),(4/scale));
                            g2.fill(s);
                        } else {
                            g2.drawLine(xVal-len, yVal-len, xVal+len, yVal+len);
                            g2.drawLine(xVal+len, yVal-len, xVal-len, yVal+len);
                        }

                        //Number
                        final AffineTransform store = g2.getTransform();
                        g2.translate(xVal, yVal);
                        g2.scale(1/scale,-1/scale);
                        g2.drawString(String.valueOf(i), 3, -3);

                        g2.setTransform(store);
                    }
                }
            }
        };
        debugGlyphDisplay.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(final MouseEvent e) {
                double eX = e.getX();
                double eY = e.getY();

                final int w = debugGlyphDisplay.getWidth();
                final int h = debugGlyphDisplay.getHeight();

                int minX = Integer.MAX_VALUE;
                int minY = Integer.MAX_VALUE;
                int maxX = Integer.MIN_VALUE;
                int maxY = Integer.MIN_VALUE;
                for (int i = 0; i < x[GLYPH_ZONE].length; i++) {
                    int val = x[GLYPH_ZONE][i];
                    if (val > maxX) {
                        maxX = val;
                    }
                    if (val < minX) {
                        minX = val;
                    }

                    val = y[GLYPH_ZONE][i];
                    if (val > maxY) {
                        maxY = val;
                    }
                    if (val < minY) {
                        minY = val;
                    }
                }

                int xRange = maxX - minX;
                int yRange = maxY - minY;

                double xScale = (double) w / xRange;
                double yScale = (double) h / yRange;
                double scale = xScale < yScale ? xScale : yScale;

                //add buffer area
                final int borderWidth = 15;
                minX -= (borderWidth/scale);
                maxX += (borderWidth/scale);
                minY -= (borderWidth/scale);
                maxY += (borderWidth/scale);

                //recalculate scale
                xRange = maxX - minX;
                yRange = maxY - minY;
                xScale = (double)w / xRange;
                yScale = (double)h / yRange;
                scale = xScale < yScale ? xScale : yScale;

                eX = (eX / scale) + minX;
                eY = h - eY;
                eY = ((eY / scale) + minY);

                debugXLabel.setText("  X: "+eX);
                debugYLabel.setText("  Y: "+eY);
            }

            @Override
            public void mouseExited(final MouseEvent e) {
                debugXLabel.setText("  X: ");
                debugYLabel.setText("  Y: ");
            }
        });
        glyphPanel.add(BorderLayout.CENTER, debugGlyphDisplay);

        showInterpolatedShadow = new JCheckBox("Show Interpolated Shadow");
        showInterpolatedShadow.setSelected(true);
        showInterpolatedShadow.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent e) {
                glyphPanel.repaint();
            }
        });
        glyphPanel.add(BorderLayout.NORTH, showInterpolatedShadow);

        debugWindow.add(BorderLayout.CENTER, glyphPanel);




        /**
         * Right panel
         */
        final JPanel dataPanel = new JPanel(){
            @Override
            public Dimension getPreferredSize() {
                final Dimension pref = super.getPreferredSize();
                final Dimension min = getMinimumSize();
                final Dimension max = getMaximumSize();

                int w = pref.width < min.width ? min.width : pref.width;
                int h = pref.height < min.height ? min.height : pref.height;

                w = w > max.width ? max.width : w;
                h = h > max.height ? max.height : h;

                return new Dimension(w, h);
            }
        };
        dataPanel.setMinimumSize(new Dimension(200,100));
        dataPanel.setMaximumSize(new Dimension(200,1000000));
        dataPanel.setLayout(new BorderLayout());
        dataPanel.setBorder(new javax.swing.border.LineBorder(Color.BLACK));

        //Stack
        final JPanel stackPanel = new JPanel(new BorderLayout());
        stackList = new JList();
        final JScrollPane stackScroll = new JScrollPane(stackList);
        stackPanel.add(BorderLayout.NORTH, new JLabel("Stack:"));
        stackPanel.add(BorderLayout.CENTER, stackScroll);

        //CVT
        final JPanel cvtPanel = new JPanel(new BorderLayout());
        cvtList = new JList(cvt.getCVTForDebug());
        final JScrollPane cvtScroll = new JScrollPane(cvtList);
        cvtPanel.add(BorderLayout.NORTH, new JLabel("CVT:"));
        cvtPanel.add(BorderLayout.CENTER, cvtScroll);

        //Storage
        final JPanel storagePanel = new JPanel(new BorderLayout());
        storageList = new JList(getStorageAsArray());
        final JScrollPane storageScroll = new JScrollPane(storageList);
        storagePanel.add(BorderLayout.NORTH, new JLabel("Storage:"));
        storagePanel.add(BorderLayout.CENTER, storageScroll);

        dataPanel.add(BorderLayout.NORTH, stackPanel);
        dataPanel.add(BorderLayout.CENTER, cvtPanel);
        dataPanel.add(BorderLayout.SOUTH, storagePanel);

        debugWindow.add(BorderLayout.EAST, dataPanel);


        /**
         * Bottom panel
         */
        final JPanel statePanel = new JPanel();
        statePanel.setLayout(new BorderLayout());
        statePanel.setBorder(new javax.swing.border.LineBorder(Color.BLACK));

        stateDisplay = new JComponent() {
            @Override
            public void paint(final Graphics g) {
                final Graphics2D g2 = (Graphics2D)g;

                g2.setPaint(Color.WHITE);
                g2.fillRect(0,0,81,81);
                g2.setPaint(Color.BLACK);
                g2.drawRect(0,0,81,81);
                g2.setPaint(Color.GRAY);
                g2.drawOval(0,0,81,81);

                g2.drawLine(0,40,5,40);
                g2.drawLine(81,40,76,40);
                g2.drawLine(40,0,40,5);
                g2.drawLine(40,81,40,76);

                g2.drawLine(12,12,15,15);
                g2.drawLine(69,12,66,15);
                g2.drawLine(12,69,15,66);
                g2.drawLine(69,69,66,66);

                //freedom vector
                g2.setPaint(new Color(0,100,0));
                int[] vec = TTGraphicsState.getVectorComponents(dGS.freedomVector);
                g2.drawLine(40,40,40+((vec[0]*40)/16384),40-((vec[1]*40)/16384));
                g2.drawString("Freedom Vector", 84, 13);
                g2.drawString("("+(vec[0]/16384d)+", "+(vec[1]/16384d)+ ')',98,23);

                //dual projection vector
                g2.setPaint(Color.BLUE);
                vec = TTGraphicsState.getVectorComponents(dGS.dualProjectionVector);
                g2.drawLine(41,41,41+((vec[0]*40)/16384),41-((vec[1]*40)/16384));
                g2.drawString("Dual Projection Vector", 84, 65);
                g2.drawString("("+(vec[0]/16384d)+", "+(vec[1]/16384d)+ ')',98,75);

                //projection vector
                g2.setPaint(Color.MAGENTA);
                vec = TTGraphicsState.getVectorComponents(dGS.projectionVector);
                g2.drawLine(41,41,41+((vec[0]*40)/16384),41-((vec[1]*40)/16384));
                g2.drawString("Projection Vector", 84, 39);
                g2.drawString("("+(vec[0]/16384d)+", "+(vec[1]/16384d)+ ')',98,49);

                //Separator
                g2.setPaint(Color.GRAY);
                g2.drawLine(240, 4, 240, 77);

                //Zone pointers
                g2.setPaint(Color.BLACK);
                g2.drawString("zp0: " + dGS.zp0 + (dGS.zp0 == 0 ? " (Twilight Zone)" : " (Glyph Zone)"), 250, 13);
                g2.drawString("zp1: " + dGS.zp1 + (dGS.zp1 == 0 ? " (Twilight Zone)" : " (Glyph Zone)"), 250, 25);
                g2.drawString("zp2: " + dGS.zp2 + (dGS.zp2 == 0 ? " (Twilight Zone)" : " (Glyph Zone)"), 250, 37);

                //Reference Points
                g2.drawString("rp0: " + dGS.rp0, 250, 51);
                g2.drawString("rp1: " + dGS.rp1, 250, 63);
                g2.drawString("rp2: " + dGS.rp2, 250, 75);

                //Separator
                g2.setPaint(Color.GRAY);
                g2.drawLine(404, 4, 404, 77);

                //Instruct Control
                g2.setPaint(Color.BLACK);
                g2.drawString("Instruct Control: "+dGS.instructControl, 414, 13);

                //Auto Flip
                g2.drawString("Auto Flip: " + dGS.autoFlip, 414, 30);

                //Deltas
                g2.drawString("Delta Base: " + dGS.deltaBase, 414, 47);
                g2.drawString("Delta Shift: " + dGS.deltaShift, 414, 59);

                //Loop
                g2.drawString("Loop: " + dGS.loop, 414, 75);

                //Separator
                g2.setPaint(Color.GRAY);
                g2.drawLine(548, 4, 548, 77);

                //Round state
                g2.setPaint(Color.BLACK);
                g2.drawString("Round State: " + dGS.getRoundStateAsString(), 558, 13);

                //Minimum distance
                g2.drawString("Minimum Distance: " + dGS.minimumDistance, 558, 30);

                //CVT cut in
                g2.drawString("CVT Cut In: " + dGS.controlValueTableCutIn, 558, 46);

                //Single Width
                g2.drawString("Single Width Cut In: " + dGS.singleWidthCutIn, 558, 63);
                g2.drawString("Single Width Value: " + dGS.singleWidthValue, 558, 75);
            }
        };
        stateDisplay.setMinimumSize(new Dimension(700,81));
        stateDisplay.setPreferredSize(new Dimension(700,81));
        stateDisplay.setMaximumSize(new Dimension(700,81));
        statePanel.add(BorderLayout.WEST, stateDisplay);

        final JPanel mousePanel = new JPanel();
        mousePanel.setLayout(new GridLayout(0,1));

        debugXLabel = new JLabel("  X: "){
            @Override
            public Dimension getPreferredSize() {
                final Dimension pref = super.getPreferredSize();
                final Dimension min = getMinimumSize();

                final int w = pref.width < min.width ? min.width : pref.width;
                final int h = pref.height < min.height ? min.height : pref.height;

                return new Dimension(w, h);
            }
        };
        debugXLabel.setBackground(Color.WHITE);
        debugXLabel.setOpaque(true);
        debugXLabel.setBorder(new javax.swing.border.LineBorder(Color.BLACK));
        debugXLabel.setMinimumSize(new Dimension(200, 20));

        debugYLabel = new JLabel("  Y: ");
        debugYLabel.setBackground(Color.WHITE);
        debugYLabel.setOpaque(true);
        debugYLabel.setBorder(new javax.swing.border.LineBorder(Color.BLACK));
        debugYLabel.setMinimumSize(new Dimension(200, 20));

        mousePanel.add(debugXLabel);
        mousePanel.add(debugYLabel);

        statePanel.add(BorderLayout.EAST, mousePanel);

        debugWindow.add(BorderLayout.SOUTH, statePanel);


        try {
            dGS = (TTGraphicsState)graphicsState.clone();
        } catch(final CloneNotSupportedException e) {
            LogWriter.writeLog("Exception: " + e.getMessage());
        }

        stack = new Stack();

        final int twilightCount = maxp.getMaxTwilightPoints();
        System.arraycopy(x[ORIGINAL+GLYPH_ZONE],0,x[GLYPH_ZONE],0,x[GLYPH_ZONE].length);
        x[TWILIGHT_ZONE] = new int[twilightCount];
        x[ORIGINAL+TWILIGHT_ZONE] = new int[twilightCount];
        System.arraycopy(y[ORIGINAL+GLYPH_ZONE],0,y[GLYPH_ZONE],0,y[GLYPH_ZONE].length);
        y[TWILIGHT_ZONE] = new int[twilightCount];
        y[ORIGINAL+TWILIGHT_ZONE] = new int[twilightCount];
        System.arraycopy(touched[ORIGINAL+GLYPH_ZONE],0,touched[GLYPH_ZONE],0,touched[GLYPH_ZONE].length);
        touched[TWILIGHT_ZONE] = new boolean[twilightCount][2];
        touched[ORIGINAL+TWILIGHT_ZONE] = new boolean[twilightCount][2];

        refreshDebugger(true);
        debugWindow.setVisible(true);
    }

    /**
     * DEBUG METHOD -
     *
     * Returns the midpoint of two values. Used when creating paths from points.
     * @param a
     * @param b
     * @return
     */
    private static int midPt(final int a, final int b) {
		return a + (b - a)/2;
	}

    /**
     * DEBUG METHOD -
     *
     * Creates paths for a given set of points, making sure not to modify any of the values passed in.
     * @param x
     * @param y
     * @param curve
     * @param contour
     * @return
     */
    private static GeneralPath getPathFromPoints(final int[] x, final int[] y, final boolean[] curve, final boolean[] contour) {
        final int ptCount=x.length;
        final int[] pX = new int[ptCount];
        System.arraycopy(x,0,pX,0,ptCount);
        final int[] pY = new int[ptCount];
        System.arraycopy(y,0,pY,0,ptCount);
        final boolean[] endOfContour = new boolean[ptCount];
        System.arraycopy(contour,0,endOfContour,0,ptCount);
        final boolean[] onCurve = new boolean[ptCount];
        System.arraycopy(curve,0,onCurve,0,ptCount);

        int start=0, firstPt=-1;
        for(int ii=0;ii<ptCount;ii++){

            if(endOfContour[ii]){

                if(firstPt!=-1 && (!onCurve[start] || !onCurve[ii]) ){ //last point not on curve and we have a first point

                    final int diff=firstPt-start;
                    int newPos;

                    //make a deep copy of values
                    final int pXlength=pX.length;
                    final int[] old_pX=new int[pXlength];
                    System.arraycopy(pX,0,old_pX,0,pXlength);

                    final int[] old_pY=new int[pXlength];
                    System.arraycopy(pY,0,old_pY,0,pXlength);

                    final boolean[] old_onCurve=new boolean[pXlength];
                    System.arraycopy(onCurve,0,old_onCurve,0,pXlength);

                    //rotate values to ensure point at start
                    for(int oldPos=start;oldPos<ii+1;oldPos++){

                        newPos=oldPos+diff;
                        if(newPos>ii) {
                            newPos -= (ii - start + 1);
                        }
                        pX[oldPos]=old_pX[newPos];
                        pY[oldPos]=old_pY[newPos];
                        onCurve[oldPos]=old_onCurve[newPos];

                    }
                }

                //reset values
                start=ii+1;
                firstPt=-1;

            }else if(onCurve[ii] && firstPt==-1){ //track first point
                firstPt=ii;
            }

        }

        boolean isFirstDraw=true;

        final GeneralPath current_path =new GeneralPath(Path2D.WIND_NON_ZERO);

        final int c= pX.length;
        int fc=-1;

        //find first end contour
        for(int jj=0;jj<c;jj++){
            if(endOfContour[jj]){
                fc=jj+1;
                jj=c;
            }
        }

        int x1,y1,x2=0,y2=0,x3=0,y3=0;

        x1=pX[0];
        y1=pY[0];

        current_path.moveTo(x1,y1);

        int xs=0,ys=0,lc=0;
        boolean isEnd=false;

        for (int j = 0; j <ptCount; j++) {

            final int p=j%fc;
            int p1=(j+1)%fc;
            int p2=(j+2)%fc;
            int pm1=(j-1)%fc;

            /**special cases
             *
             *round up to last point at end
             *First point
             */
            if(j==0) {
                pm1 = fc - 1;
            }
            if(p1<lc) {
                p1 += lc;
            }
            if(p2<lc) {
                p2 += lc;
            }

            //allow for wrap around on contour
            if(endOfContour[j]){
                isEnd=true;

                if(onCurve[fc]){
                    xs=pX[fc];
                    ys=pY[fc];
                }else{
                    xs=pX[j+1];
                    ys=pY[j+1];
                }

                //remember start point
                lc=fc;
                //find next contour
                for(int jj=j+1;jj<c;jj++){
                    if(endOfContour[jj]){
                        fc=jj+1;
                        jj=c;
                    }
                }
            }

            if(lc==fc && onCurve[p]){
                j=c;
            }else{

                if(onCurve[p] && onCurve[p1]){ //straight line
                    x3=pX[p1];
                    y3=pY[p1];
                    current_path.lineTo(x3,y3);

                    isFirstDraw=false;
                    //curves
                }else if(j<(c-3) &&((fc-lc)>1 || fc==lc)){
                    boolean checkEnd=false;
                    if(onCurve[p] && !onCurve[p1] && onCurve[p2] ){ //2 points + control

                        x1=pX[p];
                        y1=pY[p];
                        x2=pX[p1];
                        y2=pY[p1];
                        x3=pX[p2];
                        y3=pY[p2];
                        j++;
                        checkEnd=true;

                    }else if(onCurve[p] && !onCurve[p1] && !onCurve[p2]){ //1 point + 2 control

                        x1=pX[p];
                        y1=pY[p];
                        x2=pX[p1];
                        y2=pY[p1];
                        x3=midPt(pX[p1], pX[p2]);
                        y3=midPt(pY[p1], pY[p2]);
                        j++;

                        checkEnd=true;

                    }else if(!onCurve[p] && !onCurve[p1] && (!endOfContour[p2] ||fc-p2==1)){ // 2 control + 1 point (final check allows for last point to complete loop

                        x1=midPt(pX[pm1], pX[p]);
                        y1=midPt(pY[pm1], pY[p]);
                        x2=pX[p];
                        y2=pY[p];

                        x3=midPt(pX[p], pX[p1]);
                        y3=midPt(pY[p], pY[p1]);

                    }else if(!onCurve[p] && onCurve[p1]){ // 1 control + 2 point

                        x1=midPt(pX[pm1], pX[p]);
                        y1=midPt(pY[pm1], pY[p]);
                        x2=pX[p];
                        y2=pY[p];
                        x3=pX[p1];
                        y3=pY[p1];
                    }

                    if(isFirstDraw){
                        current_path.moveTo(x1,y1);
                        isFirstDraw=false;
                    }

                    if (!(endOfContour[p] && p > 0 && endOfContour[p-1])) {
                        current_path.curveTo(x1, y1, x2, y2, x3, y3);
                    }

                    /**if end after curve, roll back so we pick up the end*/
                    if( checkEnd && endOfContour[j]){

                        isEnd=true;

                        xs=pX[fc];
                        ys=pY[fc];
                        //remmeber start point
                        lc=fc;
                        //find next contour
                        for(int jj=j+1;jj<c;jj++){
                            if(endOfContour[jj]){
                                fc=jj+1;
                                jj=c;
                            }
                        }
                    }
                }

                if (endOfContour[p]) {
                    current_path.closePath();
                }


                if(isEnd){
                    current_path.moveTo(xs,ys);
                    isEnd=false;
                }
            }
        }
        return current_path;
    }

    /**
     * DEBUG METHOD -
     *
     * Runs the debugger such that it is at a specified point in the current code. This may be less than the current
     * number, in which case the glyph program is restarted and run to that point.
     * @param targetPointer
     */
    private void runDebuggerTo(final int targetPointer) {
        if (targetPointer == debugPointer) {
            return;
        }

        int targetInstr = targetPointer;
        int diff = -1;

        if (targetInstr >= 0) {
            int add = 0;
            if (targetInstr >= programToDebug.length) {
                add = targetInstr - (programToDebug.length-1);
                targetInstr = programToDebug.length-1;
            }

            while (programToDebugIsData[targetInstr]) {
                targetInstr--;
            }
            diff = targetInstr - debugPointer + add;
        }

        final int target = instructionsExecuted + diff;

        boolean skipFunctions = true;
        if (target < instructionsExecuted) {
            restartDebugger();
            skipFunctions = false;
        }

        final int startLineCount = functionsLineCount;

        debuggerRunningInBackground = true;
        while (instructionsExecuted < target + (skipFunctions ? (functionsLineCount - startLineCount) : 0)) {
            stepInto = true;
            debugPointer = process(programToDebug[debugPointer], debugPointer, programToDebug, dGS);
            debugPointer++;

            if (debugPointer == programToDebug.length && !codeStack.empty()) {
                setCurrentCodeForDebug(codeStack.pop(), numberStack.pop() + 1, false);
            }
        }
        debuggerRunningInBackground = false;

        setCurrentCodeForDebug(programToDebug, debugPointer, true);

        stepInto = false;
        instructionsExecuted = target;
    }


    /**
     * DEBUG METHOD -
     *
     * Restarts the debugger, resetting all relevant data structures.
     */
    private void restartDebugger() {

        stack = new Stack();

        try {
            dGS = (TTGraphicsState)graphicsState.clone();
        } catch(final CloneNotSupportedException e) {
            LogWriter.writeLog("Exception: " + e.getMessage());
        }

        final int twilightCount = maxp.getMaxTwilightPoints();
        System.arraycopy(x[ORIGINAL+GLYPH_ZONE],0,x[GLYPH_ZONE],0,x[GLYPH_ZONE].length);
        x[TWILIGHT_ZONE] = new int[twilightCount];
        x[ORIGINAL+TWILIGHT_ZONE] = new int[twilightCount];
        System.arraycopy(y[ORIGINAL+GLYPH_ZONE],0,y[GLYPH_ZONE],0,y[GLYPH_ZONE].length);
        y[TWILIGHT_ZONE] = new int[twilightCount];
        y[ORIGINAL+TWILIGHT_ZONE] = new int[twilightCount];
        System.arraycopy(touched[ORIGINAL+GLYPH_ZONE],0,touched[GLYPH_ZONE],0,touched[GLYPH_ZONE].length);
        touched[TWILIGHT_ZONE] = new boolean[twilightCount][2];
        touched[ORIGINAL+TWILIGHT_ZONE] = new boolean[twilightCount][2];

        if (!codeStack.isEmpty()) {
            programToDebug = codeStack.get(0);
            programToDebugIsData = getInstructionStreamIsData(programToDebug);
        }
        codeStack.clear();
        numberStack.clear();
        functionsLineCount = 0;
        debugPointer = 0;
        instructionsExecuted = 0;
        currentCode.setText("Glyph Program");
    }

    /**
     * DEBUG METHOD -
     *
     * Process the current instruction, update the display, and move the debugger onto the next.
     * @param stepIntoCall
     */
    private void advanceDebugger(final boolean stepIntoCall) {
        if (debugPointer < programToDebug.length) {
            final Thread t = new Thread() {
                @Override
                public void run() {
                    stepInto = stepIntoCall;
                    debugPointer = process(programToDebug[debugPointer], debugPointer, programToDebug, dGS);
                    stepInto = false;
                    debugPointer++;

                    refreshDebugger(false);

                    if (debugPointer == programToDebug.length && !codeStack.empty()) {
                        setCurrentCodeForDebug(codeStack.pop(), numberStack.pop() + 1, true);
                    }
                }
            };
            SwingUtilities.invokeLater(t);
        }
    }

    /**
     * DEBUG METHOD -
     *
     * Return the storage as a string array for displaying.
     * @return
     */
    private String[] getStorageAsArray() {
        final String[] result = new String[storage.length];
        for (int i=0; i<storage.length; i++) {
            result[i] = i+": "+storage[i]+"       ("+ NumberFormat.getNumberInstance().format(storage[i] / 64d)+ ')';
        }
        return result;
    }

    /**
     * DEBUG METHOD -
     *
     * Refreshes the debugger's display components.
     * @param programHasChanged
     */
    private void refreshDebugger(final boolean programHasChanged) {
        if (programHasChanged) {
            currentInstructionList.setListData(getInstructionsAsStringArray(programToDebug));
        }

        final int start = debugPointer;
        int end = debugPointer;

        while (end+1 < programToDebug.length && programToDebugIsData[end+1]) {
            end++;
        }

        currentInstructionList.setSelectionInterval(start, end);

        if (start != 0) {
            int forward = end+3;
            if (forward >= programToDebug.length) {
                forward = programToDebug.length - 1;
            }
            currentInstructionList.ensureIndexIsVisible(forward);
            int back = start - 2;
            if (back < 0) {
                back = 0;
            }
            currentInstructionList.ensureIndexIsVisible(back);
            currentInstructionList.ensureIndexIsVisible(end);
        }
        currentInstructionList.ensureIndexIsVisible(start);


        stackList.setListData(stack.toStringArray());
        cvtList.setListData(cvt.getCVTForDebug());
        storageList.setListData(getStorageAsArray());
        stateDisplay.repaint();
        debugGlyphDisplay.repaint();
    }

    /**
     * DEBUG METHOD -
     *
     * Set the program (or function etc) currently displayed.
     * @param code
     * @param pointer
     * @param updateDisplay
     */
    private void setCurrentCodeForDebug(final int[] code, final int pointer, final boolean updateDisplay) {
        programToDebug = code;
        debugPointer = pointer;

        if (!updateDisplay) {
            return;
        }

        programToDebugIsData = getInstructionStreamIsData(programToDebug);

        refreshDebugger(true);

        String function = "Glyph program";
        Object[] keySet = functions.keySet().toArray();
        for (final Object aKeySet1 : keySet) {
            //noinspection ArrayEquality
            if (functions.get(aKeySet1) == programToDebug) {
                function = "Function " + aKeySet1;
            }
        }
        keySet = instructions.keySet().toArray();
        for (final Object aKeySet : keySet) {
            //noinspection ArrayEquality
            if (instructions.get(aKeySet) == programToDebug) {
                function = "Instruction " + aKeySet;
            }
        }
        currentCode.setText(function);
    }


    /**
     * DEBUG METHOD -
     *
     * Prints out all of the coordinates in a form well suited to pasting into a spreadsheet
     */
    private void printCoords() {
        for (int i=0; i<x[GLYPH_ZONE].length; i++) {
            System.out.print(i+"\t"+x[GLYPH_ZONE][i]+ '\t' +y[GLYPH_ZONE][i]+ '\t' +x[ORIGINAL+GLYPH_ZONE][i]+ '\t' +y[ORIGINAL+GLYPH_ZONE][i]);
            System.out.println("");
            if (contour[GLYPH_ZONE][i]) {
                System.out.println();
            }
        }
        System.out.println("");
        System.out.println("");
    }


    /**
     * DEBUG METHOD -
     *
     * Prints out a set of instructions without executing them. It's not a quick method, as it uses reflection to do so!
     * @param program Instructions to print
     */
    public static void print(final int[] program) {
        System.out.println("");

        final String[] toPrint = getInstructionsAsStringArray(program);
        for (final String aToPrint : toPrint) {
            System.out.println(aToPrint);
        }

        System.out.println("");
        System.out.println("");
    }

    /**
     * DEBUG METHOD -
     *
     * Converts an array of instructions in bytecode into a human-readable string array for display.
     * @param program
     * @return
     */
    public static String[] getInstructionsAsStringArray(final int[] program) {
        if (program == null) {
            return new String[]{};
        }

        final String[] result = new String[program.length];

        final StringBuilder depth = new StringBuilder();
        final int[] notFoundCount = new int[0xFFFF];
        for (int n=0; n<program.length; n++){
            boolean found = false;
            int test = program[n];
            if (test >= MDRP && test <= MDRP+0x1F) {
                test = MDRP;
            }
            if (test >= 0xE0 && test <= 0xFF) {
                test = 0xE0;
            }

            for (Field declaredField : TTVM.class.getDeclaredFields()) {
                if (found) {
                    break;
                }
                try {

                    //ignore if not an instruction
                    if (Character.isLowerCase(declaredField.getName().charAt(0)) || declaredField.getName().contains("ZONE") || "ORIGINAL".equals(declaredField.getName())) {
                        continue;
                    }

                    if (declaredField.getInt(declaredField)==test) {
                        if ("ENDF".equals(declaredField.getName()) || "ELSE".equals(declaredField.getName()) || "EIF".equals(declaredField.getName())) {
                            depth.delete(0, 2);
                        }

                        result[n] = (n+": "+depth+ declaredField.getName());
                        if ("NPUSHB".equals(declaredField.getName())) {
                            n++;
                            final int count = program[n];
                            result[n] =(depth+"  count: "+count);
                            for (int j=0; j<count; j++) {
                                n++;
                                result[n]=(depth+"   "+program[n]);
                            }
                        } else if ("NPUSHW".equals(declaredField.getName())) {
                            n++;
                            final int count = program[n];
                            result[n]=(depth+"  count: "+count);
                            for (int j=0; j<count; j++) {
                                n++;
                                final int word = getIntFrom2Uint8(program[n], program[n+1]);
                                result[n]=(depth+"   (first half of number)");
                                n++;
                                result[n]=(depth+"   "+word);
                            }
                        } else if (program[n] >= 0xb0 && program[n] <= 0xb7) {
                            final int count = (program[n]-0xAF);
                            for (int j=0; j<count; j++) {
                                n++;
                                result[n]=(depth+"   "+program[n]);
                            }
                        } else if (program[n] >= 0xb8 && program[n] <= 0xbF) {
                            final int count = (program[n]-0xb7);
                            for (int j=0; j<count; j++) {
                                n++;
                                final int word = getIntFrom2Uint8(program[n], program[n+1]);
                                result[n]=(depth+"   (first half of number)");
                                n++;
                                result[n]=(depth+"   "+word);
                            }
                        } else if ("FDEF".equals(declaredField.getName()) || "IDEF".equals(declaredField.getName()) || "ELSE".equals(declaredField.getName()) || "IF".equals(declaredField.getName())) {
                            depth.append("  ");
                        }
                        found = true;
                    }
                } catch(final IllegalAccessException e) {
                    LogWriter.writeLog("Exception: " + e.getMessage());
                }
            }
            if (!found) {
                notFoundCount[program[n]]++;
                System.out.println(depth+"0x"+Integer.toHexString(program[n])+"    (Unimplemented)");
            }
        }

        for (int i=0; i<0xFF; i++) {
            if (notFoundCount[i] > 0) {
                System.out.println(Integer.toHexString(i) + " not found " + notFoundCount[i] + " times.");
            }
        }

        return result;
    }

    /**
     * DEBUG METHOD -
     *
     * Converts an instruction stream into a boolean array indicating whether a line is an instruction or data.
     * @param program
     * @return
     */
    public static boolean[] getInstructionStreamIsData(final int[] program) {
        if (program == null) {
            return new boolean[]{};
        }

        final boolean[] result = new boolean[program.length];

        for (int n=0; n<program.length; n++){
            boolean found = false;
            int test = program[n];
            if (test >= MDRP && test <= MDRP+0x1F) {
                test = MDRP;
            }
            if (test >= 0xE0 && test <= 0xFF) {
                test = 0xE0;
            }

            for (Field declaredField : TTVM.class.getDeclaredFields()) {
                if (found) {
                    break;
                }
                try {
                    //ignore if not an instruction
                    if (Character.isLowerCase(declaredField.getName().charAt(0)) || declaredField.getName().contains("ZONE") || "ORIGINAL".equals(declaredField.getName())) {
                        continue;
                    }

                    if (declaredField.getInt(declaredField)==test) {

                        result[n] = false;
                        if ("NPUSHB".equals(declaredField.getName())) {
                            n++;
                            final int count = program[n];
                            result[n] = true;
                            for (int j=0; j<count; j++) {
                                n++;
                                result[n]=true;
                            }
                        } else if ("NPUSHW".equals(declaredField.getName())) {
                            n++;
                            final int count = program[n];
                            result[n]=true;
                            for (int j=0; j<count; j++) {
                                n++;
                                result[n]=true;
                                n++;
                                result[n]=true;
                            }
                        } else if (program[n] >= 0xb0 && program[n] <= 0xb7) {
                            final int count = (program[n]-0xAF);
                            for (int j=0; j<count; j++) {
                                n++;
                                result[n]=true;
                            }
                        } else if (program[n] >= 0xb8 && program[n] <= 0xbF) {
                            final int count = (program[n]-0xb7);
                            for (int j=0; j<count; j++) {
                                n++;
                                result[n]=true;
                                n++;
                                result[n]=true;
                            }
                        }
                        found = true;
                    }
                } catch(final IllegalAccessException e) {
                    LogWriter.writeLog("Exception: " + e.getMessage());
                }
            }
        }

        return result;
    } 
}
