;Algomusic for Ataxia II
;Usage:
;new_music(path$, seed) 	- initialize music
;music(music) 		- call during game runtime
;update_music(music) - call at intervals throughout the game

;To do: balance complexities of melodies (repetitiveness vs. irregularity and tendency towards base notes vs. other notes)

;;;;;;;;;;C6 pitches *10
Dim hz#(12)
hz#(0)=10465.0 ; C
hz#(1)=11087.3 ; C#
hz#(2)=11746.6 ; D
hz#(3)=12445.1 ; D#
hz#(4)=13185.1 ; E
hz#(5)=13969.1 ; F
hz#(6)=14799.8 ; F#
hz#(7)=15679.8 ; G
hz#(8)=16612.2 ; G#
hz#(9)=17600.0 ; A
hz#(10)=18646.6; A#
hz#(11)=19755.3; B


;;;;;;;;;;; set up constants

Const ROOT = 0
Const PER_2ND = 2
Const MIN_3RD = 3
Const MAJ_3RD = 4
Const PER_4TH = 5
Const DIM_5TH = 6
Const PER_5TH = 7
Const PER_6TH = 9
Const MIN_7TH = 10
Const MAJ_7th = 11
Const PER_12TH = 12

Const DEFAULT_PATH$ = "music/"

;;;;;;;;;;; set up chord structures

.chord
Const MINOR=0 : 	Data MIN_3RD, PER_5TH, PER_12TH
Const MAJOR=1 : 	Data MAJ_3RD, PER_5TH, PER_12TH
Const DIMINISHED=2:	Data MIN_3RD, DIM_5TH, PER_12TH
Const SUSPENDED=3:	Data PER_4TH, PER_5TH, PER_12TH
Const MINOR_7TH=4:  Data MAJ_3RD, PER_5TH, MIN_7TH
Data 0,0,0

Restore chord
Dim chords(6,3)
Dim chordname$(6)
n=-1
Repeat
	n=n+1
	Read chords(n,1), chords(n,2), chords(n,3)
Until chords(n,1)=0

;;;;;;;;;;; define types

Type melody
	Field seq$
	Field seed
	Field tempo
	Field oct#
	Field sound
	Field channel
	Field pointer
	Field rests ; how many rests played since last note
	Field optvolume# ; optimum volume
	Field volume# ; current volume
	Field dv# ; rate of change of volume
	Field volumeEnvelope#
	Field newNoteAction
	Field pan#
	Field targetLength
	Field duplex, channel2 ; used for harmonies within a single melody
	
	Field altsound ; alternate b section
	Field altseq$
	Field alttype ; 0=none, 1=altsound, 2=altseq
	Field altphase ;0 or 1, toggles at end of each sequence
	Field file$
	Field altfile$
	
	Field lastVolume# ; modified volume of last note played
	Field lastNote
End Type

Type music
	Field tempo
	Field time
	Field ticks
	Field cMelodies ; melody count
	Field root ; root note
	Field chord
	Field quiet ; time signature of the "emphasis" notes - possibly move to melodies?
	Field seed
	Field TARGET_VOLUME#
	Field TARGET_OCT#
	Field TARGET_LENGTH
	Field quietVolume#
	Field duplexVolume#
	Field order
	Field complex		;order = tendency for even length melodies and regular chords; complex = tendendy for greater number of melodies
	Field path$ ; pointer to sample directory 
	Field nSamples ; number of sample files in path$
	Field bLockFirstMelody ; if true, the first melody never changes.
	Field bFadeMelody	   ; if true, fade melodies in and out
	
	Field probCopySequence		;chance of a new sequence copying existing sequence
	Field probCopySequenceChange;chance of a copied sequence changing the notes
	Field probUseMotif
	
	Field motifSeq$ ; sequence for a motif.
End Type

Function new_music.music(path$,seed)
	;initialise a new "music" type. There should only be one of these.
	
	Delete Each music
	For ml.melody = Each melody : remove_melody(ml) : Next
	
	music.music = New music
	
	;prepare samples
	music\path$ = path$
	If FileType(music\path$)=0
		RuntimeError "You need make a music/ subfolder and fill it with .wav files!"
	EndIf
	dir = ReadDir(CurrentDir$()+music\path$)
	music\nSamples = 0
	done=False
	Repeat
		file$ = NextFile$(dir)
		If valid_sound_file(music\path$+file$)
			music\nSamples = music\nSamples + 1
		Else If file$=""
			done=True
		EndIf
	Until done=True
	CloseDir dir
	
	music\seed = seed
	SeedRnd(music\seed)
	
	music\tempo = 300 + Rand(-90,30) ; higher = slower
	music\time = MilliSecs()
	music\root = Rand(0,11)
	music\chord = set_chord(music)
	music\quiet=1^Rand(0,2) 
	music\TARGET_VOLUME# = 1.0	
	music\TARGET_OCT# = 0.5
	music\TARGET_LENGTH = 8
	music\bLockFirstMelody = True
	music\bFadeMelody = True
	music\probCopySequence = 2
	music\probCopySequenceChange = 2
	music\probUseMotif=Rand(1,6)
	music\quietVolume# = 0.5
	music\duplexVolume# = 0.8
	
	mLength = Rand(1,2)+Rand(1,3)
	For n=1 To mLength
		music\motifSeq$ = music\motifSeq$ + random_note$(True)
	Next
	
	If music\nSamples = 0
	EndIf
	
	Return music
End Function

Function update_music(music.music)
;;;;;;;;;;; introduce changes based on game state
;;;;;;;;;;; "order" is on the scale of -400 to +400
;;;;;;;;;;; "complex" can be any number from 0 upwards
	If music = Null Then Return
	set_chord(music)
	
	If First melody = Null
		ml.melody = new_melody(music)
	EndIf
	
	;possibly alter existing melodies
	ml.melody = First melody
	If music\bLockFirstMelody Then ml = After ml
	If ml<>Null
		r=Rand(1,10)
		Select r
		Case 1: ;replace with new melody
			fade_out_melody(music,ml)
			ml.melody = new_melody(music)
		Case 2: ; change octave
			If ml\oct# < 1
				ml\oct# = ml\oct#*2
			Else
				ml\oct# = ml\oct#/2
			EndIf
		Case 3 : ;change tempo
			ml\tempo = Rand(0,2)
			ml\seq$ = make_sequence(ml,ml\seed,music)
		Case 4
			ml\duplex = Rand(1,6)
		Case 5 ; new melody
			ml\seed = music\seed + music\ticks
			ml\seq$ = make_sequence(ml,ml\seed,music)
		Case 6 ; keep sequence, change notes
			ml\seq$ = change_sequence(ml,music)
		End Select
	EndIf
	
	;alter number of melodies based on complexity
	num = 1
	For ml.melody = Each melody : num=num *2 :Next
	If music\complex > num Or num=1
		ml.melody = new_melody(music)
	Else If music\complex < num-1 And num>2
		ml.melody = First melody
		If music\bLockFirstMelody Then ml = After ml
		fade_out_melody(music,ml)
	EndIf
End Function

Function fade_out_melody(music.music,ml.melody)
			If music\bFadeMelody = True
				ml\dv# = -0.1
			Else
				ml\volume# = 0
			EndIf
End Function
			
Function music(music.music)
	;returns true if this is a tick in which pointers are advanced
	If music=Null Then Return False
	If MilliSecs() >= music\time + music\tempo
		music\time = MilliSecs()
		music\ticks = music\ticks + 1
		;play melodies
		For ml.melody = Each melody
			;play the note
			ml\pointer=ml\pointer+1
			If ml\pointer>Len(current_seq$(ml))
				ml\pointer=1 ; loop melody
				ml\altphase = 1-ml\altphase
			EndIf
			note(music,ml\oct#,music\chord,ml)
			
			;handle volumes
			ml\volume# = ml\volume# + ml\dv#
			If ml\volume# >= ml\optvolume#
				ml\dv#=0
				ml\volume# = ml\optvolume#
			Else If ml\volume# <=0 And ChannelPlaying(ml\channel)=False
				remove_melody(ml)
			EndIf
		Next
		Return True
	Else
		Return False		
	EndIf
End Function

Function current_seq$(ml.melody)
	If ml\alttype = 2 And ml\altphase = 1
		Return ml\altseq$
	Else
		Return ml\seq$
	EndIf
End Function

Function set_chord(music.music)
	r = music\order + Rand(-100,100)
	If r<0
		music\chord = DIMINISHED 
	Else If r<50
		music\chord = MINOR 
	Else If r<150
		music\chord = MINOR_7TH 
	Else If r < 300
		music\chord = SUSPENDED 
	Else
		music\chord = MAJOR 
	EndIf
End Function

Function clear_all_channels()
	For ml.melody = Each melody
		remove_melody(ml)
	Next
End Function

Function refresh_channels()
	For ml.melody = Each melody
		StopChannel ml\channel
	Next
End Function

Function note(music.music,oct#,chord,ml.melody)
	ml\lastNote = 0
	ml\lastVolume# = 0
	If ml\pointer<0 Then Return
	If ml\pointer>sequence_length(ml) Then Return
	If ml\volume# <=0 Then Return
	
	c$=Mid$(current_seq$(ml),ml\pointer,1)
	
	a=Asc(c$)
	If a=0 ; rest
		ml\rests = ml\rests + 1
		Return
	EndIf
	
	If a=1 Or a=2 Or a=3
		note = music\root + chords(chord,a)
	Else
		note = music\root + a - 100
	EndIf
	
	
	If ml\rests = 0 And ChannelPlaying(ml\channel)=True And ml\pointer<>1
		continueNote = True
	Else
		continueNote = False
	EndIf
	
	If ml\NewNoteAction = 2 And continueNote Then StopChannel(ml\channel)
		;new note stop 
	If ml\NewNoteAction = 1 And continueNote
		;new note slide
	Else
		;normal
		sound = ml\sound
		If ml\altphase = 1 And ml\alttype = 1 Then sound = ml\altsound
		ml\channel = PlaySound(sound)
	EndIf
	
	;pitch and volume
	v#=ml\volume# + Rnd(-0.1,0.1)
	p#=ml\pointer#
	If p#/music\quiet <> Int(p#/music\quiet) Then v#=v# * music\quietVolume#
	If ml\duplex<>0 Then v#=v# * music\duplexVolume#
	Select ml\volumeEnvelope
		Case 1: v# = v# * (0.75+Sin(180 * ml\pointer/sequence_length(ml))/4)
		Case 2: v# = v# * (0.75+Cos(180 * ml\pointer/sequence_length(ml))/4)
		Case 3: v# = v# * (0.75+Sin(360 * ml\pointer/sequence_length(ml))/4)
		Case 4: v# = v# * (0.75+Cos(360 * ml\pointer/sequence_length(ml))/4)
	End Select
	
	ChannelVolume ml\channel,v#
	
	ChannelPitch ml\channel, pitch(note)*oct#
	
	If ml\duplex<>0
		ml\channel2 = PlaySound(ml\sound)
		ChannelVolume ml\channel,v#/2
		ChannelVolume ml\channel2,v#/2
		Select ml\duplex
		Case 1: ChannelPitch ml\channel2,pitch(note)*oct#*2
		Case 2: ChannelPitch ml\channel2,pitch(music\root)
		Case 3: ChannelPitch ml\channel2,pitch(music\root+chords(chord,2))
		End Select
	EndIf
	
	ml\rests = 0
	ml\lastVolume# = v#
	ml\lastNote = note

End Function

Function pitch(n)
	m#=1
	Repeat
		If n<0
			n=n+12
			m#=m#/2
		EndIf
		If n>=12
			n=n-12
			m#=m#*2
		EndIf
	Until n>=0 And n<=11
	Return hz#(n) * m# * 2
End Function

Function getsound$(music.music)
	r = Rand(1,music\nSamples)
	dir = ReadDir(CurrentDir$()+music\path$)
	n=0
	Repeat
		file$ = NextFile$(dir)
		If valid_sound_file(music\path$+file$) 
			n=n+1
		EndIf
	Until n=r
	CloseDir dir
	Return file$
End Function

Function valid_sound_file(file$)

	If FileType(file$) = 1 And Right$(file$,4)=".wav"
		Return True
	Else	
		Return False
	EndIf
End Function

Function new_melody.melody(music.music)
	;assess existing
	totalpan#=0
	totalvolume#=0
	totaloct#=0
	mcount=0
	totallength#=0
	totaltempo#=0

	For ml.melody = Each melody : mcount=mcount+1
		totalpan#    =totalpan#    + ml\pan# * ml\volume#; needs to average at 0
		totalvolume# =totalvolume# + ml\volume# 
		totaloct#    =totaloct#    + ml\oct# 
		totallength# =totallength# + ml\targetLength
		totaltempo#  =totaltempo#  + ml\tempo
	Next
	
	If mcount>0
		totalpan# = totalpan# / mcount
		totaloct# = totaloct# / mcount
		totallength# = totallength# / mcount
		totaltempo# = totaltempo# / mcount
	EndIf
	
	;assign new
	ml.melody = New melody
	ml\seq$=""
	ml\seed = music\seed + music\ticks + music\cMelodies
	ml\oct# = 1
	ml\alttype = Rand(0,2)
	ml\NewNoteAction = 0
	If Rand(0,3)=0 Then ml\NewNoteAction=1
	ml\volumeEnvelope=Rand(0,4)

	ml\file$ = music\path$ + getsound(music)
	ml\sound = LoadSound(ml\file$)
	
	ml\altfile$ = music\path$ + getsound(music)
	ml\altsound = LoadSound(ml\altfile$)
	
	;set volume, octave and pan
	ratio# = music\TARGET_VOLUME# / totalvolume#
	If ratio# > 1.0 Then ratio# = 1.0
	ml\optvolume# = ratio#
	ml\optvolume# = ml\optvolume * Rnd(0.8,1.2)
	ml\volume# = ml\optvolume#
	
	If totaloct# < music\TARGET_OCT#
		If Rand(0,3) = 0 : ml\oct# = ml\oct# / 2 : EndIf
	Else 
		If Rand(0,3) > 1 : ml\oct# = ml\oct# / 2 : EndIf
	EndIf
	If ml\oct#>=1
		r=Rand(1,6)
		Select r
		Case 1,2,3:ml\duplex=r
		End Select
	EndIf
	
	If totalpan# < 0
		ml\pan# = Rnd(0,0.8)
	Else If totalpan# > 0
		ml\pan# = Rnd(-0.8,0)
	Else
		ml\pan# = Rnd(-0.5,0.5)
	EndIf
	
	If totallength# > music\TARGET_LENGTH * 1.2
		ml\targetLength = Rand(2,music\TARGET_LENGTH)
	Else If totallength# < music\TARGET_LENGTH * 0.8
		ml\targetLength = Rand(music\TARGET_LENGTH,music\TARGET_LENGTH*2)
	Else
		ml\targetLength = music\TARGET_LENGTH * Rnd(0.5,1.5)
	EndIf
	
	If totaltempo# >= 1.5
		ml\tempo = Rand(0,1)
	Else If totaltempo# <= 0.5
		ml\tempo = Rand(1,2)
	Else
		ml\tempo = Rand(1,1)
	EndIf
	
	If music\order>50 :ml\targetLength = ml\targetLength/2 : ml\targetlength=ml\targetlength*2 : EndIf
	If music\order>200:ml\targetLength = ml\targetLength/4 : ml\targetlength=ml\targetlength*4 : EndIf
	If ml\targetLength<2 Then ml\targetLength = 2
	

	ml\seq$ 	= make_sequence(ml,ml\seed	,music)
	If Rand(0,1)=0
		ml\altseq$ 	= make_sequence(ml,ml\seed+1,music) ; different sequence
	Else
		ml\altseq$ = change_sequence$(ml,music) ; same sequence, different notes
	EndIf
	
	;set fade in
	If music\bFadeMelody
		ml\dv# = 0.1
		ml\volume# = 0
	Else
		ml\dv# = 0
		ml\volume# = 1.0
	EndIf
	
	music\cMelodies = music\cMelodies + 1
		
	Return ml
End Function

Function remove_melody(ml.melody)
	StopChannel ml\channel
	FreeSound ml\sound
	FreeSound ml\altsound
	Delete ml
End Function


Function make_sequence$(ml.melody,seed,music.music)
	;creates a sequence
	;format is a string, with each asc(char) representing a note
	;0 = rest
	;1 = chord 3rd
	;2 = chord 5th
	;3 = chord 7th
	;100 = chord base note
	;111 to 112 = any other note up to an octave above. 
	seq$=""
	SeedRnd(seed)
	
	;check for copying existing melody
	If ml<>First melody
		If Rand(0,music\probCopySequence)=0
			ml2.melody = Before ml
			If Rand(0,music\probCopySequenceChange)=0
				seq$ = change_sequence(ml2,music)
			Else
				seq$ = ml2\seq$
			EndIf
			Return seq$
		EndIf
	EndIf
	
	;standard sequence creation
	filler$=Chr$(0)
	If Abs(music\order)>=200+Rand(0,400)
		filler$ = Chr$(100+chords(music\chord,Rand(0,3)))
		If Rand(0,2)=0 Then filler$="foo"
	EndIf

	useMotif = False : pMotif = 0
	n = 0
	Repeat
		n=n+1
		If useMotif=False
			mChance=Rand(1,6) + music\probUseMotif
			If n=1 Or n=ml\targetLength/2 Or n=ml\targetLength/4 Then mchance=mchance + 2
			If mChance>=7 Then useMotif=True
		EndIf
		If useMotif
			pMotif=pMotif + 1
			If pMotif <= Len(music\motifSeq$)
				z$=z$+Mid$(music\motifSeq$,pMotif,1)
			Else
				useMotif=False
				pMotif=0
			EndIf
		Else
			z$ = random_note$(True)
		EndIf
		If filler$ = "foo" Then filler$ = z$
		If ml\tempo>0
			r=Rand(0,2)
			If r=0
				z$ = filler$ + z$
			Else
				z$ = z$ + filler$
			EndIf
		EndIf
		If ml\tempo=2
			z$ = Left$(z$,1)+Chr$(0)+Right$(z$,1)+Chr$(0)
		EndIf
		seq$=seq$+z$
	Until Len(seq$) >= ml\targetLength
	If seq$ > ml\targetLength Then seq$=Left$(seq$,ml\targetLength)
	Return seq$
End Function

Function change_sequence$(ml.melody,music.music)
	;returns ml\seq$ with alternate notes
	seq$=""
	For n=1 To Len(ml\seq$)
		If ml\seq$<>Chr$(0)
			seq$=seq$+random_note$(False)
		Else
			seq$=seq$+Mid$(ml\seq$,n,1)
		EndIf
	Next
	Return seq$
End Function

Function random_note$(bIncludeRest)
	If bIncludeRest=False
		r=Rand(5,30)
	Else
		r=Rand(0,30)
	EndIf
	Select r
	Case 0,1,2,3,4: z$=Chr$(0)
	Case 5,6,7,8,9: z$=Chr$(1)
	Case 10,11,12,13,14: z$=Chr$(2)
	Case 15,16,17,18,19: z$=Chr$(3)
	Case 20: z$=Chr$(100)
	Case 21: z$=Chr$(100+PER_2ND)
	Case 22: z$=Chr$(100+MIN_3RD)
	Case 23: z$=Chr$(100+MAJ_3RD)
	Case 24: z$=Chr$(100+PER_4TH)
	Case 25: z$=Chr$(100+PER_5TH)
	Case 26: z$=Chr$(100+DIM_5TH)
	Case 27: z$=Chr$(100+MIN_7TH)
	Case 28: z$=Chr$(100+MAJ_7TH)
	Case 29: z$=Chr$(100+PER_12TH)
	Case 30: z$=Chr$(100+PER_6TH)
	End Select
	Return z$
End Function

Function sequence_length(ml.melody)
	;returns length of melody's current sequence
	If ml\alttype=2 And ml\altphase = 1
		Return Len(ml\altseq$)
	Else
		Return Len(ml\seq$)
	EndIf
End Function

Function any_old_music(music.music)
	music\order = 0
	music\complex = 10
	update_music(music)
End Function