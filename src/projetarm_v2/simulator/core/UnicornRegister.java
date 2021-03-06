/*
 * Copyright (c) 2018-2019 Valentin D'Emmanuele, Gilles Mertens, Dylan Fraisse, Hugo Chemarin, Nicolas Gervasi
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package projetarm_v2.simulator.core;

import unicorn.Unicorn;

public class UnicornRegister implements Register {
	private final Unicorn u;
	private int register;
	
	public UnicornRegister(Unicorn u, int register) {
		this.u = u;
		this.register = register;
	}
	
	/* (non-Javadoc)
	 * @see projetarm_v2.Registers#getValue()
	 */
	@Override
	public int getValue() {
		return ((Long)u.reg_read(this.register)).intValue();
	}
	
	/* (non-Javadoc)
	 * @see projetarm_v2.Registers#setValue(long)
	 */
	@Override
	public void setValue(int value) {
		u.reg_write(this.register, value);
	}
}
