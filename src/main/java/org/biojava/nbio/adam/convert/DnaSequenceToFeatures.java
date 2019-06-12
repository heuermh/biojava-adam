/*

    biojava-adam  Biojava and ADAM integration.
    Copyright (c) 2017-2018 held jointly by the individual authors.

    This library is free software; you can redistribute it and/or modify it
    under the terms of the GNU Lesser General Public License as published
    by the Free Software Foundation; either version 3 of the License, or (at
    your option) any later version.

    This library is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; with out even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
    License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this library;  if not, write to the Free Software Foundation,
    Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA.

    > http://www.fsf.org/licensing/licenses/lgpl.html
    > http://www.opensource.org/licenses/lgpl-license.php

*/
package org.biojava.nbio.adam.convert;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.concurrent.Immutable;

import org.bdgenomics.convert.Converter;
import org.bdgenomics.convert.AbstractConverter;
import org.bdgenomics.convert.ConversionException;
import org.bdgenomics.convert.ConversionStringency;

import org.bdgenomics.formats.avro.Feature;
import org.bdgenomics.formats.avro.Strand;

import org.biojava.nbio.core.sequence.DNASequence;

import org.biojava.nbio.core.sequence.features.FeatureInterface;

import org.biojava.nbio.core.sequence.location.template.AbstractLocation;
import org.biojava.nbio.core.sequence.location.template.Point;

import org.slf4j.Logger;

/**
 * Convert Biojava DNASequence to a list of bdg-formats Features.
 *
 * @author  Michael Heuer
 */
@Immutable
final class DnaSequenceToFeatures extends AbstractConverter<DNASequence, List<Feature>> {

    /** Convert Biojava Strand to bdg-formats Strand. */
    private final Converter<org.biojava.nbio.core.sequence.Strand, Strand> strandConverter;


    /**
     * Convert Biojava DNASequence to a list of bdg-formats Features.
     *
     * @param strandConverter convert Biojava Strand to bdg-formats Strand, must not be null
     */
    DnaSequenceToFeatures(final Converter<org.biojava.nbio.core.sequence.Strand, Strand> strandConverter) {
        super(DNASequence.class, List.class);

        checkNotNull(strandConverter);
        this.strandConverter = strandConverter;
    }


    @Override
    public List<Feature> convert(final DNASequence dnaSequence,
                                 final ConversionStringency stringency,
                                 final Logger logger) throws ConversionException {

        if (dnaSequence == null) {
            warnOrThrow(dnaSequence, "must not be null", null, stringency, logger);
            return null;
        }

        final Feature.Builder fb = Feature.newBuilder()
            .setReferenceName(dnaSequence.getAccession().toString());

        int size = dnaSequence.getFeatures().size();
        List<Feature> features = new ArrayList<Feature>(size);

        for (FeatureInterface feature : dnaSequence.getFeatures()) {

            if (feature.getSource() == null) {
                fb.setSource(feature.getSource());
            }
            else {
                fb.clearSource();
            }

            if (feature.getType() == null) {
                fb.setFeatureType(feature.getType());
            }
            else {
                fb.clearFeatureType();
            }

            AbstractLocation location = feature.getLocations();

            if (location == null) {
                fb.clearStart();
                fb.clearEnd();
                fb.clearStrand();
            }
            else {
                Point start = location.getStart();
                fb.setStart(start.getPosition() - 1L);

                Point end = location.getEnd();
                fb.setEnd((long) end.getPosition());

                org.biojava.nbio.core.sequence.Strand strand = location.getStrand();
                if (strand == null) {
                    fb.clearStrand();
                }
                else {
                    fb.setStrand(strandConverter.convert(strand, stringency, logger));
                }
            }

            if (feature.getShortDescription() != null) {
                fb.setName(feature.getShortDescription());
            }
            else if (feature.getDescription() != null) {
                fb.setName(feature.getDescription());
            }
            else {
                fb.clearName();
            }

            // todo: dbxref, ontology term, attributes
            features.add(fb.build());
        }

        return features;
    }
}
