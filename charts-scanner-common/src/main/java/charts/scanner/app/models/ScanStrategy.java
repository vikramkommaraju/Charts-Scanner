package charts.scanner.app.models;

/**
 * Enumeration of all the strategies used to scan stocks
 * 
 * @author vkommaraju
 *
 */
public enum ScanStrategy {

	EMA_55_CROSSOVER("https://stockcharts.com/def/servlet/SC.uscan?r=1644581"),
	HIGH_VOLUME_55_CROSSOVER("https://stockcharts.com/def/servlet/SC.uscan?r=1658303"),
	NEW_52_WEEK_HIGHS("https://stockcharts.com/def/servlet/SC.scan?s=TSAL[t.t_eq_s]![as0,20,tv_gt_40000]![th0_gt_am1,253,th]&report=predefall"),
	VOLUME_GAINERS("https://stockcharts.com/def/servlet/SC.scan?s=TSAL[t.t_eq_s]![as0,20,tv_gt_40000]![tv0_gt_as1,20,tv*4]![tc0_gt_tc1]&report=predefall"),
	NEW_CCI_BUY("https://stockcharts.com/def/servlet/SC.scan?s=TSAL[t.t_eq_s]![as0,20,tv_gt_40000]![bu0,20_gt_100]![bu1,20_le_100]![bu2,20_lt_100]&report=predefall"),
	BULLISH_STOCASTIC_POP("https://stockcharts.com/def/servlet/SC.uscan?r=1643698"),
	GOLDEN_CROSSOVER("https://stockcharts.com/def/servlet/SC.scan?s=TSAL[t.t_eq_s]![as0,20,tv_gt_40000]![as0,50,tc_gt_as0,200,tc]![as1,50,tc_le_as1,200,tc]&report=predefall"),
	BULLISH_MACD_CROSSOVER("https://stockcharts.com/def/servlet/SC.scan?s=TSAL[t.t_eq_s]![as0,20,tv_gt_40000]![ba0_lt_0]![ba0_ge_bb0]![ba1_lt_bb1]![ba2_lt_bb2]![ba3_lt_bb3]&report=predefall"),
	OVERSOLD_WITH_IMPROVING_RSI("https://stockcharts.com/def/servlet/SC.scan?s=TSAL[t.t_eq_s]![as0,20,tv_gt_40000]![bs0_gt_30]![bs1_lt_29]![bs2_lt_28]![bs3_lt_27]&report=predefall"),
	NEW_UPTREND_ADX("https://stockcharts.com/def/servlet/SC.scan?s=TSAL[t.t_eq_s]![as0,20,tv_gt_40000]![bm0,14_gt_20]![bm1,14_le_20]![bm2,14_le_20]![bn0,14_gt_bo0,14]&report=predefall"),
	GAP_UPS("https://stockcharts.com/def/servlet/SC.scan?s=TSAL[t.t_eq_s]![as0,20,tv_gt_40000]![tl0_gt_th1*1.026]&report=predefall"),
	BREAKAWAY_GAP_UPS("https://stockcharts.com/def/servlet/SC.scan?s=TSAL[t.t_eq_s]![as0,20,tv_gt_40000]![tl0_gt_th1*1.026]&report=predefall"),
	ICHIMOKU_BUY("https://stockcharts.com/def/servlet/SC.uscan?r=1642651"),
	ISLAND_BOTTOM("https://stockcharts.com/def/servlet/SC.scan?s=TSAL[t.t_eq_s]![as0,20,tv_gt_40000]![tc2_lt_ax2,10,tc]![th1_lt_tl2*0.975]![tl1_ne_th1]![tl0_gt_th1*1.025]&report=predefall"),
	RUNAWAY_GAP_UPS("https://stockcharts.com/def/servlet/SC.scan?s=TSAL[t.t_eq_s]![as0,20,tv_gt_40000]![tc1_gt_ax1,10,tc]![tl0_gt_th1*1.025]![tl0_ne_th0]&report=predefall"),
	NEW_UPTREND_AROON("https://stockcharts.com/def/servlet/SC.scan?s=TSAL[t.t_eq_s]![as0,20,tv_gt_40000]![br0_gt_50]![br1_le_50]&report=predefall"),
	PARABOLIC_BUY("https://stockcharts.com/def/servlet/SC.scan?s=TSAL[t.t_eq_s]![as0,20,tv_gt_40000]![ap1,0.02,0.2_gt_tc1]![ap0,0.02,0.2_lt_tc0]&report=predefall"),
	IMPROVING_CHAIKIN_MONEY_FLOW("https://stockcharts.com/def/servlet/SC.scan?s=TSAL[t.t_eq_s]![as0,20,tv_gt_40000]![bt0,20_ge_0.2]![bt1,20_lt_0.2]![bt2,20_lt_0.2]![bt3,20_lt_0.2]![bt4,20_lt_0.2]&report=predefall"),
	;
	
	String endpointUrl;
	
	ScanStrategy(String url) {
		this.endpointUrl = url;
	}
	
	public String url() {
		return this.endpointUrl;
	}
	
}
