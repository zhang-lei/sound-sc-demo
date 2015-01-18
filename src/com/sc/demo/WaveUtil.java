package com.sc.demo;

/**
 * 波形编码工具类 <p/>
 * 包含 字符编码 解码
 *
 * @author zhanglei31
 * @created 2015-01-07
 */
public class WaveUtil {

    /**
     * 波特率
     */
    public static final int baudRate = 16000;
    
    /**
     * 将一个字节封装成一个数据包
     * <p/>
     * 将<code>data</code>转换成一个二进制字符串
     * 然后 拼成   起始位(0) 数据位(8bit) 校验位  停止位 这样的格式
     * e.g.  0 10000100 0 111
     * 
     * @param data byte 值
     * @return 返回 byte转换成的数据包
     * 
     * @author zhanglei4517@gmail.com
     * @date 2015-01-05
     * 
     */
    public static StringBuffer byte2package(final byte data) {
    	
    	// 转换成二进制字符串
        String binary = Integer.toBinaryString(data);

        // 偶校验 记录 计数的个数
        int evenCheckCnt = 0;
        
        /*
         *  校验位(偶校验)
         *  如果是 1 的位数为偶数  even 为 0 否则 为1
         */
        char even = '0';
        for (int k = 0; k < binary.length(); k++) {
            char c = binary.charAt(k);
            if (c == '1') {
                evenCheckCnt ++;
            }
        }
        
        if (evenCheckCnt % 2 == 1) {
            even = '1';
        }
        
        /*
         * 如果不足8位 进行补零操作
         * 补齐8位 如果高位为1 去掉高位
         */
        int count = binary.length();
        if (count == 8) {
            binary = binary.substring(1);
        }
        for (int i = 0; i < 8 - count; i++) {
        	binary = "0" + binary;
        }

        /*
         * 封装包格式
         * 0(起始位 1bit) 数据位(8bit) even(校验位 1bit) 111 (结束位)
         */
        StringBuffer sdata = new StringBuffer();
        sdata.append(Integer.toBinaryString(0x07));  // 结束位
        sdata.append(even);                          // 校验位
        sdata.append(binary);                        // 数据位
        sdata.append(0);                             // 起始位                           
        
        return sdata.reverse();                      // 翻转数据 => 正确的数据格式
    }
    
    
    /**
     * 将字节数组转换成一个连续的数据包
     * @param datas 字节数组
     * @param byteCnt 字节数组的长度
     * @return 返回包格式
     * 
     * @author zhanglei
     * @date 2015-01-05
     */
    public static StringBuffer bytes2package(final byte[] datas, int byteCnt) {
    	
    	StringBuffer strPackage = new StringBuffer();
    	
    	/*
    	 *  数据见补位
    	 *  多个数据之间存在间隔 32 个 1(空位 标识没有数据)
    	 */
    	String gap = Integer.toBinaryString(0xffffffff);
    	
    	/*
    	 * 前面补充16个空位
    	 */
    	strPackage.append(gap);
    	
    	 // 重复64次
        for (int i= 0; i < byteCnt; i++) {
        	strPackage.append(byte2package(datas[i]));
        }

        /*
         * 后面补充128位
         */
        strPackage.append(gap).append(gap).append(gap).append(gap);
        
        return strPackage;
    	
    }
    
    /**
     * 将包格式 转换成 波形数组
     * 
     * @param pkg 包格式字符串
     * @param bitcount 半个波长的位数
     * @return 返回波形数组
     * 
     * @author zhanglei
     * @date 2015-01-05
     * 
     */
    public static short[] package2wave(String pkg, int bitcount) {
    	
    	if (pkg == null) {
    		return null;
    	}

    	pkg = pkg.replaceAll("0", "lh");
    	pkg = pkg.replaceAll("1", "hl");

    	pkg = pkg.replaceAll("h", "1");
    	pkg = pkg.replaceAll("l", "0");

        // 转换
        int cnt = pkg.length();  // 长度

        /*
         * 创建新的字节数组
         * 存放转化后的波形
         */
        short[] newbuf = new short[cnt * bitcount];         // 记录最大数字
        int bufindex = 0;
        for (int i = 0; i < cnt; i++) {
            
            char c = pkg.charAt(i);
            if ( c == '0') {
                for (int k = 0; k < bitcount; k ++) {
                    newbuf[bufindex++] = 0x7fff;
                }
            } else {
                for (int k = 0; k < bitcount; k ++) {
                    newbuf[bufindex++] = -0x7fff;
                }
            }
        }
        return newbuf;
    }
    
    /**
     * 将字节数组转换成为波形数组(方波 等频)
     * @param datas 字节数组
     * @param byteCnt 字节数组的长度
     * @param bitcount
     * @return 返回波形数组
     * 
     * @author zhanglei
     * @date 2015-01-05
     */
    public static short[] byte2wave(final byte[] datas, int byteCnt, int bitcount) {
    	
    	/*
    	 * 获取字节数组的包格式 
    	 */
    	StringBuffer strPackage = bytes2package(datas, byteCnt);
    	
    	return package2wave(strPackage.toString(), bitcount);
    	
    }
}
