package com.carrotsearch.labs.langid;

import java.io.IOException;

import com.carrotsearch.labs.langid.LzmaBase;

class LzmaDecoder
{
	static class LenDecoder
	{
		short[] m_Choice = new short[2];
		LzmaBitTreeDecoder[] m_LowCoder = new LzmaBitTreeDecoder[LzmaBase.kNumPosStatesMax];
		LzmaBitTreeDecoder[] m_MidCoder = new LzmaBitTreeDecoder[LzmaBase.kNumPosStatesMax];
		LzmaBitTreeDecoder m_HighCoder = new LzmaBitTreeDecoder(LzmaBase.kNumHighLenBits);
		int m_NumPosStates = 0;
		
		public void Create(int numPosStates)
		{
			for (; m_NumPosStates < numPosStates; m_NumPosStates++)
			{
				m_LowCoder[m_NumPosStates] = new LzmaBitTreeDecoder(LzmaBase.kNumLowLenBits);
				m_MidCoder[m_NumPosStates] = new LzmaBitTreeDecoder(LzmaBase.kNumMidLenBits);
			}
		}
		
		public void Init()
		{
			com.carrotsearch.labs.langid.LzmaBitDecoder.InitBitModels(m_Choice);
			for (int posState = 0; posState < m_NumPosStates; posState++)
			{
				m_LowCoder[posState].Init();
				m_MidCoder[posState].Init();
			}
			m_HighCoder.Init();
		}
		
		public int Decode(com.carrotsearch.labs.langid.LzmaBitDecoder rangeDecoder, int posState) throws IOException
		{
			if (rangeDecoder.DecodeBit(m_Choice, 0) == 0)
				return m_LowCoder[posState].Decode(rangeDecoder);
			int symbol = LzmaBase.kNumLowLenSymbols;
			if (rangeDecoder.DecodeBit(m_Choice, 1) == 0)
				symbol += m_MidCoder[posState].Decode(rangeDecoder);
			else
				symbol += LzmaBase.kNumMidLenSymbols + m_HighCoder.Decode(rangeDecoder);
			return symbol;
		}
	}
	
	static class LiteralDecoder
	{
		class Decoder2
		{
			short[] m_Decoders = new short[0x300];
			
			public void Init()
			{
				com.carrotsearch.labs.langid.LzmaBitDecoder.InitBitModels(m_Decoders);
			}
			
			public byte DecodeNormal(com.carrotsearch.labs.langid.LzmaBitDecoder rangeDecoder) throws IOException
			{
				int symbol = 1;
				do
					symbol = (symbol << 1) | rangeDecoder.DecodeBit(m_Decoders, symbol);
				while (symbol < 0x100);
				return (byte)symbol;
			}
			
			public byte DecodeWithMatchByte(com.carrotsearch.labs.langid.LzmaBitDecoder rangeDecoder, byte matchByte) throws IOException
			{
				int symbol = 1;
				do
				{
					int matchBit = (matchByte >> 7) & 1;
					matchByte <<= 1;
					int bit = rangeDecoder.DecodeBit(m_Decoders, ((1 + matchBit) << 8) + symbol);
					symbol = (symbol << 1) | bit;
					if (matchBit != bit)
					{
						while (symbol < 0x100)
							symbol = (symbol << 1) | rangeDecoder.DecodeBit(m_Decoders, symbol);
						break;
					}
				}
				while (symbol < 0x100);
				return (byte)symbol;
			}
		}
		
		Decoder2[] m_Coders;
		int m_NumPrevBits;
		int m_NumPosBits;
		int m_PosMask;
		
		public void Create(int numPosBits, int numPrevBits)
		{
			if (m_Coders != null && m_NumPrevBits == numPrevBits && m_NumPosBits == numPosBits)
				return;
			m_NumPosBits = numPosBits;
			m_PosMask = (1 << numPosBits) - 1;
			m_NumPrevBits = numPrevBits;
			int numStates = 1 << (m_NumPrevBits + m_NumPosBits);
			m_Coders = new Decoder2[numStates];
			for (int i = 0; i < numStates; i++)
				m_Coders[i] = new Decoder2();
		}
		
		public void Init()
		{
			int numStates = 1 << (m_NumPrevBits + m_NumPosBits);
			for (int i = 0; i < numStates; i++)
				m_Coders[i].Init();
		}
		
		Decoder2 GetDecoder(int pos, byte prevByte)
		{
			return m_Coders[((pos & m_PosMask) << m_NumPrevBits) + ((prevByte & 0xFF) >>> (8 - m_NumPrevBits))];
		}
	}
	
	LzmaOutWindow m_OutWindow = new LzmaOutWindow();
	com.carrotsearch.labs.langid.LzmaBitDecoder m_RangeDecoder = new com.carrotsearch.labs.langid.LzmaBitDecoder();
	
	short[] m_IsMatchDecoders = new short[LzmaBase.kNumStates << LzmaBase.kNumPosStatesBitsMax];
	short[] m_IsRepDecoders = new short[LzmaBase.kNumStates];
	short[] m_IsRepG0Decoders = new short[LzmaBase.kNumStates];
	short[] m_IsRepG1Decoders = new short[LzmaBase.kNumStates];
	short[] m_IsRepG2Decoders = new short[LzmaBase.kNumStates];
	short[] m_IsRep0LongDecoders = new short[LzmaBase.kNumStates << LzmaBase.kNumPosStatesBitsMax];
	
	LzmaBitTreeDecoder[] m_PosSlotDecoder = new LzmaBitTreeDecoder[LzmaBase.kNumLenToPosStates];
	short[] m_PosDecoders = new short[LzmaBase.kNumFullDistances - LzmaBase.kEndPosModelIndex];
	
	LzmaBitTreeDecoder m_PosAlignDecoder = new LzmaBitTreeDecoder(LzmaBase.kNumAlignBits);
	
	LenDecoder m_LenDecoder = new LenDecoder();
	LenDecoder m_RepLenDecoder = new LenDecoder();
	
	LiteralDecoder m_LiteralDecoder = new LiteralDecoder();
	
	int m_DictionarySize = -1;
	int m_DictionarySizeCheck =  -1;
	
	int m_PosStateMask;
	
	public LzmaDecoder()
	{
		for (int i = 0; i < LzmaBase.kNumLenToPosStates; i++)
			m_PosSlotDecoder[i] = new LzmaBitTreeDecoder(LzmaBase.kNumPosSlotBits);
	}
	
	boolean SetDictionarySize(int dictionarySize)
	{
		if (dictionarySize < 0)
			return false;
		if (m_DictionarySize != dictionarySize)
		{
			m_DictionarySize = dictionarySize;
			m_DictionarySizeCheck = Math.max(m_DictionarySize, 1);
			m_OutWindow.Create(Math.max(m_DictionarySizeCheck, (1 << 12)));
		}
		return true;
	}
	
	boolean SetLcLpPb(int lc, int lp, int pb)
	{
		if (lc > LzmaBase.kNumLitContextBitsMax || lp > 4 || pb > LzmaBase.kNumPosStatesBitsMax)
			return false;
		m_LiteralDecoder.Create(lp, lc);
		int numPosStates = 1 << pb;
		m_LenDecoder.Create(numPosStates);
		m_RepLenDecoder.Create(numPosStates);
		m_PosStateMask = numPosStates - 1;
		return true;
	}
	
	void Init() throws IOException
	{
		m_OutWindow.Init(false);
		
		com.carrotsearch.labs.langid.LzmaBitDecoder.InitBitModels(m_IsMatchDecoders);
		com.carrotsearch.labs.langid.LzmaBitDecoder.InitBitModels(m_IsRep0LongDecoders);
		com.carrotsearch.labs.langid.LzmaBitDecoder.InitBitModels(m_IsRepDecoders);
		com.carrotsearch.labs.langid.LzmaBitDecoder.InitBitModels(m_IsRepG0Decoders);
		com.carrotsearch.labs.langid.LzmaBitDecoder.InitBitModels(m_IsRepG1Decoders);
		com.carrotsearch.labs.langid.LzmaBitDecoder.InitBitModels(m_IsRepG2Decoders);
		com.carrotsearch.labs.langid.LzmaBitDecoder.InitBitModels(m_PosDecoders);
		
		m_LiteralDecoder.Init();
		int i;
		for (i = 0; i < LzmaBase.kNumLenToPosStates; i++)
			m_PosSlotDecoder[i].Init();
		m_LenDecoder.Init();
		m_RepLenDecoder.Init();
		m_PosAlignDecoder.Init();
		m_RangeDecoder.Init();
	}
	
	public boolean Code(java.io.InputStream inStream, java.io.OutputStream outStream,
			long outSize) throws IOException
	{
		m_RangeDecoder.SetStream(inStream);
		m_OutWindow.SetStream(outStream);
		Init();
		
		int state = LzmaBase.StateInit();
		int rep0 = 0, rep1 = 0, rep2 = 0, rep3 = 0;
		
		long nowPos64 = 0;
		byte prevByte = 0;
		while (outSize < 0 || nowPos64 < outSize)
		{
			int posState = (int)nowPos64 & m_PosStateMask;
			if (m_RangeDecoder.DecodeBit(m_IsMatchDecoders, (state << LzmaBase.kNumPosStatesBitsMax) + posState) == 0)
			{
				LiteralDecoder.Decoder2 decoder2 = m_LiteralDecoder.GetDecoder((int)nowPos64, prevByte);
				if (!LzmaBase.StateIsCharState(state))
					prevByte = decoder2.DecodeWithMatchByte(m_RangeDecoder, m_OutWindow.GetByte(rep0));
				else
					prevByte = decoder2.DecodeNormal(m_RangeDecoder);
				m_OutWindow.PutByte(prevByte);
				state = LzmaBase.StateUpdateChar(state);
				nowPos64++;
			}
			else
			{
				int len;
				if (m_RangeDecoder.DecodeBit(m_IsRepDecoders, state) == 1)
				{
					len = 0;
					if (m_RangeDecoder.DecodeBit(m_IsRepG0Decoders, state) == 0)
					{
						if (m_RangeDecoder.DecodeBit(m_IsRep0LongDecoders, (state << LzmaBase.kNumPosStatesBitsMax) + posState) == 0)
						{
							state = LzmaBase.StateUpdateShortRep(state);
							len = 1;
						}
					}
					else
					{
						int distance;
						if (m_RangeDecoder.DecodeBit(m_IsRepG1Decoders, state) == 0)
							distance = rep1;
						else
						{
							if (m_RangeDecoder.DecodeBit(m_IsRepG2Decoders, state) == 0)
								distance = rep2;
							else
							{
								distance = rep3;
								rep3 = rep2;
							}
							rep2 = rep1;
						}
						rep1 = rep0;
						rep0 = distance;
					}
					if (len == 0)
					{
						len = m_RepLenDecoder.Decode(m_RangeDecoder, posState) + LzmaBase.kMatchMinLen;
						state = LzmaBase.StateUpdateRep(state);
					}
				}
				else
				{
					rep3 = rep2;
					rep2 = rep1;
					rep1 = rep0;
					len = LzmaBase.kMatchMinLen + m_LenDecoder.Decode(m_RangeDecoder, posState);
					state = LzmaBase.StateUpdateMatch(state);
					int posSlot = m_PosSlotDecoder[LzmaBase.GetLenToPosState(len)].Decode(m_RangeDecoder);
					if (posSlot >= LzmaBase.kStartPosModelIndex)
					{
						int numDirectBits = (posSlot >> 1) - 1;
						rep0 = ((2 | (posSlot & 1)) << numDirectBits);
						if (posSlot < LzmaBase.kEndPosModelIndex)
							rep0 += LzmaBitTreeDecoder.ReverseDecode(m_PosDecoders,
									rep0 - posSlot - 1, m_RangeDecoder, numDirectBits);
						else
						{
							rep0 += (m_RangeDecoder.DecodeDirectBits(
									numDirectBits - LzmaBase.kNumAlignBits) << LzmaBase.kNumAlignBits);
							rep0 += m_PosAlignDecoder.ReverseDecode(m_RangeDecoder);
							if (rep0 < 0)
							{
								if (rep0 == -1)
									break;
								return false;
							}
						}
					}
					else
						rep0 = posSlot;
				}
				if (rep0 >= nowPos64 || rep0 >= m_DictionarySizeCheck)
				{
					// m_OutWindow.Flush();
					return false;
				}
				m_OutWindow.CopyBlock(rep0, len);
				nowPos64 += len;
				prevByte = m_OutWindow.GetByte(0);
			}
		}
		m_OutWindow.Flush();
		m_OutWindow.ReleaseStream();
		m_RangeDecoder.ReleaseStream();
		return true;
	}
	
	public boolean SetDecoderProperties(byte[] properties)
	{
		if (properties.length < 5)
			return false;
		int val = properties[0] & 0xFF;
		int lc = val % 9;
		int remainder = val / 9;
		int lp = remainder % 5;
		int pb = remainder / 5;
		int dictionarySize = 0;
		for (int i = 0; i < 4; i++)
			dictionarySize += ((int)(properties[1 + i]) & 0xFF) << (i * 8);
		if (!SetLcLpPb(lc, lp, pb))
			return false;
		return SetDictionarySize(dictionarySize);
	}
}
