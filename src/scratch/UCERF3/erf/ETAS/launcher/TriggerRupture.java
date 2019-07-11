package scratch.UCERF3.erf.ETAS.launcher;

import java.util.ArrayList;
import java.util.List;

import org.opensha.commons.geo.Location;
import org.opensha.commons.util.FaultUtils;
import org.opensha.refFaultParamDb.vo.FaultSectionPrefData;
import org.opensha.sha.faultSurface.CompoundSurface;
import org.opensha.sha.faultSurface.RuptureSurface;
import org.opensha.sha.faultSurface.SimpleFaultData;
import org.opensha.sha.faultSurface.StirlingGriddedSurface;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;

import scratch.UCERF3.FaultSystemRupSet;
import scratch.UCERF3.erf.ETAS.ETAS_EqkRupture;

public abstract class TriggerRupture {
	
	final Long customOccurrenceTime;

	protected TriggerRupture(Long customOccurrenceTime) {
		this.customOccurrenceTime = customOccurrenceTime;
	}
	
	public long getOccurrenceTime(long simulationStartTime) {
		if (customOccurrenceTime != null && customOccurrenceTime > Long.MIN_VALUE) {
			Preconditions.checkState(customOccurrenceTime <= simulationStartTime,
					"Trigger rupture custom occurrence time (%s) cannot be after simulation start time (%s)",
					customOccurrenceTime, simulationStartTime);
			return customOccurrenceTime;
		}
		return simulationStartTime;
	}
	
	public abstract ETAS_EqkRupture buildRupture(FaultSystemRupSet rupSet, long simulationStartTime);
	
	public abstract int[] getSectionsRuptured(FaultSystemRupSet rupSet);
	
	public static class FSS extends TriggerRupture {
		
		final int fssIndex;
		final Double overrideMag;
		
		public FSS(int fssIndex) {
			this(fssIndex, null, null);
		}

		public FSS(int fssIndex, Long customOccurrenceTime, Double overrideMag) {
			super(customOccurrenceTime);
			this.fssIndex = fssIndex;
			this.overrideMag = overrideMag;
		}

		@Override
		public ETAS_EqkRupture buildRupture(FaultSystemRupSet rupSet, long simulationStartTime) {
			double origMag = rupSet.getMagForRup(fssIndex);
			
			long ot = getOccurrenceTime(simulationStartTime);
			
			System.out.println("Building FSS rupture with index="+fssIndex+", OT="+ot+", original M="+(float)origMag);
			
			ETAS_EqkRupture mainshockRup = new ETAS_EqkRupture();
			mainshockRup.setOriginTime(ot);
			mainshockRup.setAveRake(rupSet.getAveRakeForRup(fssIndex));
			mainshockRup.setMag(origMag);
			mainshockRup.setRuptureSurface(rupSet.getSurfaceForRupupture(fssIndex, 1d, false));
			mainshockRup.setFSSIndex(fssIndex);
			
			if (overrideMag != null && overrideMag > 0) {
				System.out.println("\tOverriding magnitude with specified mag: "+overrideMag);
				mainshockRup.setMag(overrideMag);
			}
			
			return mainshockRup;
		}

		@Override
		public int[] getSectionsRuptured(FaultSystemRupSet rupSet) {
			return Ints.toArray(rupSet.getSectionsIndicesForRup(fssIndex));
		}
		
	}
	
	public static class SectionBased extends TriggerRupture {
		
		final int[] subSects;
		final double mag;

		public SectionBased(int[] subSects, Long customOccurrenceTime, double mag) {
			super(customOccurrenceTime);
			Preconditions.checkState(subSects.length > 0, "Must supply at least 1 subsection index!");
			this.subSects = subSects;
			this.mag = mag;
		}

		@Override
		public ETAS_EqkRupture buildRupture(FaultSystemRupSet rupSet, long simulationStartTime) {
			long ot = getOccurrenceTime(simulationStartTime);
			
			System.out.println("Building rupture from "+subSects.length+" specified subsections with OT="+ot+" and M="+(float)mag);
			
			ETAS_EqkRupture mainshockRup = new ETAS_EqkRupture();
			mainshockRup.setOriginTime(ot);
			mainshockRup.setMag(mag);
			
			List<RuptureSurface> rupSurfs = new ArrayList<>();
			List<Double> rakes = new ArrayList<>();
			List<Double> areas = new ArrayList<>();
			double gridSpacing = 1;
			for (int sectIndex : subSects) {
				Preconditions.checkState(sectIndex >= 0 && sectIndex < rupSet.getNumSections(),
						"Bad subsection index. %s is outside of bounts [0, %s]", sectIndex, rupSet.getNumSections()-1);
				FaultSectionPrefData fltData = rupSet.getFaultSectionData(sectIndex);
				rakes.add(fltData.getAveRake());
				areas.add(fltData.getReducedDownDipWidth()*fltData.getTraceLength());
				rupSurfs.add(fltData.getStirlingGriddedSurface(gridSpacing, false, true));
			}
			if (rupSurfs.size() == 1) {
				mainshockRup.setAveRake(rakes.get(0));
				mainshockRup.setRuptureSurface(rupSurfs.get(0));
			} else {
				mainshockRup.setAveRake(FaultUtils.getInRakeRange(FaultUtils.getScaledAngleAverage(areas, rakes)));
				mainshockRup.setRuptureSurface(new CompoundSurface(rupSurfs));
			}
			
			return mainshockRup;
		}

		@Override
		public int[] getSectionsRuptured(FaultSystemRupSet rupSet) {
			return subSects;
		}
		
	}
	
	public static class Point extends TriggerRupture {
		
		final Location hypocenter;
		final double mag;

		public Point(Location hypocenter, Long customOccurrenceTime, double mag) {
			super(customOccurrenceTime);
			this.hypocenter = hypocenter;
			this.mag = mag;
		}

		@Override
		public ETAS_EqkRupture buildRupture(FaultSystemRupSet rupSet, long simulationStartTime) {
			long ot = getOccurrenceTime(simulationStartTime);
			
//			System.out.println("Building point rupture hypo="+hypocenter+", OT="+ot+", M="+(float)mag);
			
			ETAS_EqkRupture mainshockRup = new ETAS_EqkRupture();
			mainshockRup.setOriginTime(ot);
			mainshockRup.setAveRake(0.0); // not used
			mainshockRup.setMag(mag);
			mainshockRup.setPointSurface(hypocenter);
			mainshockRup.setHypocenterLocation(hypocenter);
			
			return mainshockRup;
		}

		@Override
		public int[] getSectionsRuptured(FaultSystemRupSet rupSet) {
			return null;
		}
		
	}
	
	public static class SimpleFault extends TriggerRupture {
		
		public final SimpleFaultData[] sfds;
		public final Location hypo;
		public final double mag;
		public final int[] sectsReset;

		public SimpleFault(Long customOccurrenceTime, Location hypo, double mag, SimpleFaultData... sfds) {
			this(customOccurrenceTime, hypo, mag, null, sfds);
		}

		public SimpleFault(Long customOccurrenceTime, Location hypo, double mag, int[] sectsReset, SimpleFaultData... sfds) {
			super(customOccurrenceTime);
			Preconditions.checkState(sfds.length > 0, "Must supply at least 1 subsection index!");
			this.sfds = sfds;
			this.hypo = hypo;
			this.mag = mag;
			this.sectsReset = sectsReset;
		}

		@Override
		public ETAS_EqkRupture buildRupture(FaultSystemRupSet rupSet, long simulationStartTime) {
			long ot = getOccurrenceTime(simulationStartTime);
			
			System.out.println("Building rupture from "+sfds.length+" simple faults OT="+ot+" and M="+(float)mag);
			
			ETAS_EqkRupture mainshockRup = new ETAS_EqkRupture();
			mainshockRup.setOriginTime(ot);
			mainshockRup.setMag(mag);
			mainshockRup.setHypocenterLocation(hypo);
			
			List<RuptureSurface> rupSurfs = new ArrayList<>();
			double gridSpacing = 1;
			for (SimpleFaultData sfd : sfds)
				rupSurfs.add(new StirlingGriddedSurface(sfd, gridSpacing, gridSpacing));
			if (rupSurfs.size() == 1)
				mainshockRup.setRuptureSurface(rupSurfs.get(0));
			else
				mainshockRup.setRuptureSurface(new CompoundSurface(rupSurfs));
			
			return mainshockRup;
		}

		@Override
		public int[] getSectionsRuptured(FaultSystemRupSet rupSet) {
			return sectsReset;
		}
		
	}

}
