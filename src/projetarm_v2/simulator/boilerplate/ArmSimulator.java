package projetarm_v2.simulator.boilerplate;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import projetarm_v2.simulator.core.Assembler;
import projetarm_v2.simulator.core.Cpu;
import projetarm_v2.simulator.core.InvalidAssemblyException;
import projetarm_v2.simulator.core.Program;
import projetarm_v2.simulator.core.Ram;
import projetarm_v2.simulator.utils.NativeJarGetter;
import unicorn.UnicornException;

/**
 * ArmSimulator is class responsible for handling the creation of the main ARM
 * Simulator classes.
 */
public class ArmSimulator {
	
	static {
		try {
			NativeJarGetter.getInstance().loadLibraryFromJar("libunicorn_java");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	/**
	 * The current loaded program
	 */
	private final Program program;

	private final Assembler assembler;
	/**
	 * The cpu to execute the prorgam
	 */
	private Cpu cpu;

	private Ram ram;

	private Map<Integer, Integer> asmToLine;

	private final static Pattern labelPattern = Pattern.compile("([a-zA-Z]+:)");

	/**
	 * Creates a arm simulator ready to use, with all the needed components (cpu,
	 * program, linesMap, interpretor)
	 */
	public ArmSimulator() {
		this.assembler = Assembler.getInstance();
		this.program = new Program();
		this.ram = new Ram();
		this.cpu = new Cpu(ram, Cpu.DEFAULT_STARTING_ADDRESS, 2 * 1024 * 1024); // 2 MB of RAM
		this.asmToLine = new HashMap<>();
	}

	public void setProgram(String assembly) throws InvalidInstructionException {
		assembly = assembly.replace(System.lineSeparator(), ";");

		try {
			fillRamWithAssembly(assembly);
		} catch (InvalidAssemblyException e) {
		}

		fillAddressLineMap(assembly);
	}

	private String fillRamWithAssembly(String assembly) {
		int startingAddress = (int) this.cpu.getStartingAddress();
		this.asmToLine.clear();

		byte[] binary = (this.assembler.assemble(assembly, startingAddress));

		for (int i = 0; i < binary.length; i++) {
			this.ram.setByte(startingAddress + i, binary[i]);
		}
		
		this.cpu.setEndAddress(startingAddress + binary.length);
		
		return assembly;
	}

	private void fillAddressLineMap(String assembly) {
		
		int currentLine = 1;
		int currentAddress = (int)this.cpu.getStartingAddress();

		Matcher matcher = labelPattern.matcher(assembly);

		StringBuilder labelsBuilder = new StringBuilder();
		while (matcher.find()) {
			labelsBuilder.append(matcher.group());
		}
		String labels = labelsBuilder.toString();
		
		for (String line : assembly.split(";")) {
			if (line != "") {
				byte[] lineBytes;
				try {
					lineBytes = (this.assembler.assemble(labels + line.substring(Math.abs(line.indexOf(':') + 1)),
						currentAddress));
				} catch (InvalidAssemblyException e) {
					throw new InvalidInstructionException("[ERROR] " + e.getMessage() + " @ Line " + currentLine, currentLine);
				}
				asmToLine.put(currentAddress, currentLine);
				currentAddress += lineBytes.length - (line.contains("=") ? 1 : 0) * 4;
				currentLine += 1;
			}
		}
	}

	/**
	 * Returns the register value corresponding to the given number
	 */
	public int getRegisterValue(int registerNumber) {
		return this.cpu.getRegister(registerNumber).getValue();
	}

	public Ram getRam() {
		return this.ram;
	}

	public Cpu getCpu() {
		return this.cpu;
	}

	/**
	 * Returns a byte(8bits) from the ram corresponding to the given address
	 */
	public byte getRamByte(long address) {
		return this.ram.getByte(address);
	}

	/**
	 * Returns a half-word(16bits) from the ram corresponding to the given address
	 */
	public short getRamHWord(long address) {
		return this.ram.getHWord(address);
	}

	/**
	 * Returns a word(32bits) from the ram corresponding to the given address
	 */
	public int getRamWord(long address) {
		return this.ram.getValue(address);
	}

	/**
	 * Starting the processor to the next break or to the end
	 */
	public void run() {
		try {
			this.cpu.runAllAtOnce();
		} catch (UnicornException e) {
			this.handleException(e);
		}
	}

	/**
	 * Staring the processor to execute a single instruction
	 */
	public void runStep() {
		try {
			this.cpu.runStep();
		} catch (UnicornException e) {
			this.handleException(e);
		}
	}

	// TODO Convert to an exception so it can be handled as wished by the UIs
	private void handleException(UnicornException e) {
		System.out.format("[ERROR] %s @ Instruction [Address=0x%x, Line=%d]\n[ERROR] EMULATION ABORTED!\n", e.getMessage(),
				this.getRegisterValue(15), this.getCurrentLine());
	}

	public int getCurrentLine() {
		return this.asmToLine.getOrDefault(this.getRegisterValue(15), 0);
	}

	/**
	 * Resets the execution (clears the current execution point)
	 */
	public void resetRun() {
		this.cpu.getRegister(15).setValue(this.cpu.getStartingAddress());
	}

	public void resetState() {
		this.ram = new Ram();
		this.cpu = new Cpu(ram, Cpu.DEFAULT_STARTING_ADDRESS, 2 * 1024 * 1024);
	}

	/**
	 * Returns the Negative Flag status
	 */
	public boolean getN() {
		return this.cpu.getCPSR().n();
	}

	/**
	 * Returns the Zero Flag status
	 */
	public boolean getZ() {
		return this.cpu.getCPSR().z();
	}

	/**
	 * Returns the Carry Flag status
	 */
	public boolean getC() {
		return this.cpu.getCPSR().c();
	}

	/**
	 * Returns the oVerflow Flag status
	 */
	public boolean getV() {
		return this.cpu.getCPSR().v();
	}

	public boolean getQ() {
		return this.cpu.getCPSR().q();
	}

	/**
	 * Returns true if the cpu is halted
	 */
	public boolean isRunning() {
		return this.cpu.isRunning() && !this.cpu.hasFinished();
	}

	public boolean hasFinished() {
		return this.cpu.hasFinished();
	}

	/**
	 * Stops the execution
	 */
	public void interruptExecutionFlow() {
		this.cpu.interruptMe();
	}
}