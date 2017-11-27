package com.steinwurf.score.shared;
/*-
 * Copyright (c) 2017 Steinwurf ApS
 * All Rights Reserved
 *
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF STEINWURF
 * The copyright notice above does not evidence any
 * actual or intended publication of such source code.
 */

/**
 * Nalu Type enum
 */
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

    /**
     * Parses the NALU header and returns the NALU type.
     * @param data The data containing the NALU
     * @return the NALU type.
     */
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
