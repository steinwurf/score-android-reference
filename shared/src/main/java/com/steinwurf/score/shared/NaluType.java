package com.steinwurf.score.shared;

public enum NaluType {

        Unspecified0,
        NonIdrSlice,
        ASlice,
        BSlice,
        CSlice,
        IdrSlice,
        Sei,
        SequenceParameterSet,
        PictureParameterSet,
        AccessUnitDelimiter,
        EndOfSequence,
        EndOfStream,
        FillerData,
        SpsExtension,
        PrefixNalUnit,
        SubsetSequenceParameterSet,
        DepthParameterSet,
        Reserved17,
        Reserved18,
        AuxiliarySlice,
        Extension,
        DepthViewSlice,
        Reserved22,
        Reserved23,
        Unspecified24,
        Unspecified25,
        Unspecified26,
        Unspecified27,
        Unspecified28,
        Unspecified29,
        Unspecified30,
        Unspecified31,
        Unknown;


    public static NaluType parse(byte[] data)
    {
        if (data.length < 5)
            throw new IllegalArgumentException("Buffer too short");

        // Assuming Nalu header of size 4 (it can also be of size 5)
        if (data[0] != 0x00 || data[1] != 0x00 || data[2] != 0x00 || data[3] != 0x01)
        {
            throw new IllegalArgumentException("Nalu header not found");
        }

        int typeId = (data[4] & 0x1F);
        return typeId > NaluType.values().length ? NaluType.Unknown : NaluType.values()[typeId];
    }
}
