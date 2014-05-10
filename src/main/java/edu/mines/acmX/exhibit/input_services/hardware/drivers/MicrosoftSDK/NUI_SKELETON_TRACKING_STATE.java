package edu.mines.acmX.exhibit.input_services.hardware.drivers.MicrosoftSDK;

public enum NUI_SKELETON_TRACKING_STATE implements JnaEnum<NUI_SKELETON_TRACKING_STATE>  {

	NUI_SKELETON_NOT_TRACKED(0),
	NUI_SKELETON_POSITION_ONLY( NUI_SKELETON_NOT_TRACKED.value + 1 ) ,
	NUI_SKELETON_TRACKED( NUI_SKELETON_POSITION_ONLY.value + 1 );

	public int value;
	private NUI_SKELETON_TRACKING_STATE(int value) {
		this.value = value;
	}

	public int getIntValue() {
		return value;
	}

	public NUI_SKELETON_TRACKING_STATE getForValue(int i) {
		for (NUI_SKELETON_TRACKING_STATE x : values()) {
			if (x.value == i) {
				return x;
			}
		}
		return null;
	}
}
