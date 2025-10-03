// ============================================================================
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2022
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ============================================================================
package hiconic.rx.explorer.processing.platformreflection.system;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;

/**
 * More or less the idea from
 * <a href="https://github.com/oshi/oshi/blob/master/oshi-demo/src/main/java/oshi/demo/DetectVM.java">DetectVM.java</a>
 * <br/>
 * The MAC ranges might be updated in the future - e.g. <a href=
 * "https://www.techrepublic.com/blog/data-center/mac-address-scorecard-for-common-virtual-machine-platforms/">Common
 * Virtual Machines MAC addresses</a>
 * 
 *
 */
public class VirtualMachineDetector {
	// Constant for Mac address OUI portion, the first 24 bits of MAC address
	// https://www.webopedia.com/TERM/O/OUI.html
	private static final Map<String, String> vmMacAddressOUI = new HashMap<>();

	private static final String LINUX_KVM = "Linux KVM";
	private static final String LINUX_LGUEST = "Linux lguest";
	private static final String OPEN_VZ = "OpenVZ";
	private static final String QEMU = "Qemu";
	private static final String MS_VIRTUAL_PC = "Microsoft Virtual PC";
	private static final String VMWARE = "VMWare";
	private static final String LINUX_VSERVER = "linux-vserver";
	private static final String XEN = "Xen";
	private static final String FREEBSD_JAIL = "FreeBSD Jail";
	private static final String VIRTUALBOX = "VirtualBox";
	private static final String PARALLELS = "Parallels";
	private static final String LINUX_CONTAINERS = "Linux Containers";
	private static final String LXC = "LXC";

	private static final String VMWARE_ESX_3_ = "VMware ESX 3";
	private static final String MS_HYPER_V_ = "Microsoft Hyper-V";
	private static final String PARALLELS_DESKTOP_ = "Parallels Desktop";
	private static final String VIRTUAL_IRON_4_ = "Virtual Iron 4";
	private static final String XEN_OR_ORACLE_VM_ = "Xen or Oracle VM";
	private static final String VIRTUALBOX_ = "VirtualBox";
	private static final String DOCKER_CONTAINER_ = "Docker Container";

	private static final String KUBERNETES = "Kubernetes";

	static {
		vmMacAddressOUI.put("00:50:56", VMWARE_ESX_3_);
		vmMacAddressOUI.put("00:0C:29", VMWARE_ESX_3_);
		vmMacAddressOUI.put("00:05:69", VMWARE_ESX_3_);
		vmMacAddressOUI.put("00:03:FF", MS_HYPER_V_);
		vmMacAddressOUI.put("00:1C:42", PARALLELS_DESKTOP_);
		vmMacAddressOUI.put("00:0F:4B", VIRTUAL_IRON_4_);
		vmMacAddressOUI.put("00:16:3E", XEN_OR_ORACLE_VM_);
		vmMacAddressOUI.put("08:00:27", VIRTUALBOX_);
		vmMacAddressOUI.put("02:42:AC", DOCKER_CONTAINER_);
	}

	//@formatter:off
	private static final String[] vmModelArray = new String[] { 
			LINUX_KVM, 
			LINUX_LGUEST, 
			OPEN_VZ, 
			QEMU, 
			MS_VIRTUAL_PC, 
			VMWARE,
			LINUX_VSERVER, 
			XEN, 
			FREEBSD_JAIL, 
			VIRTUALBOX, 
			PARALLELS, 
			LINUX_CONTAINERS, 
			LXC 
		};
	//@formatter:on
	// -----------------------------------------------------------------------
	// PUBLIC METHODS
	// -----------------------------------------------------------------------

	/**
	 * The function attempts to identify which Virtual Machine (VM) based on common VM signatures in MAC address and
	 * computer model.
	 *
	 * @return A string indicating the machine's virtualization info if it can be determined, or an emptry string
	 *         otherwise.
	 */
	public String identifyVM(HardwareAbstractionLayer hw) {

		// Try well known MAC addresses
		List<NetworkIF> nifs = hw.getNetworkIFs();

		for (NetworkIF nif : nifs) {
			String mac = nif.getMacaddr().toUpperCase();
			String oui = findOuiByMacAddressIfPossible(mac);
			if (oui != null && !oui.isEmpty()) {
				return oui;
			}
		}

		// Try well known models
		String model = hw.getComputerSystem().getModel();
		for (String vm : vmModelArray) {
			if (model.contains(vm)) {
				return vm;
			}
		}
		String manufacturer = hw.getComputerSystem().getManufacturer();
		if ("Microsoft Corporation".equals(manufacturer) && "Virtual Machine".equals(model)) {
			return "Microsoft Hyper-V";
		}

		try (Stream<String> stream = Files.lines(Paths.get("/proc/1/cgroup"))) {
			if (stream.anyMatch(line -> line.contains("/docker"))) {
				return DOCKER_CONTAINER_;
			}
		} catch (Exception e) {
			// ignore - not found
		}

		try (Stream<String> stream = Files.lines(Paths.get("/proc/1/cgroup"))) {
			if (stream.anyMatch(line -> line.contains("/kubepods"))) {
				return KUBERNETES;
			}
		} catch (Exception e) {
			// ignore - not found
		}

		// Couldn't find VM, return empty string
		return "unknown/not virtualized";
	}

	// -----------------------------------------------------------------------
	// HELPER METHODS
	// -----------------------------------------------------------------------

	/**
	 * <p>
	 * findOuiByMacAddressIfPossible.
	 * </p>
	 *
	 * @param mac
	 *            a {@link java.lang.String} object.
	 * @return a {@link java.lang.String} object.
	 */
	private String findOuiByMacAddressIfPossible(String mac) {
		return vmMacAddressOUI.entrySet().stream().filter(entry -> mac.startsWith(entry.getKey())).map(Map.Entry::getValue)
				.collect(Collectors.joining());
	}
}
