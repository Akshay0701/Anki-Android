package com.ichi2.libanki;

import com.ichi2.anki.RobolectricTest;
import com.ichi2.utils.JSONObject;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collections;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class CollectionTest extends RobolectricTest {

    @Test
    public void editClozeGenerateCardsInSameDeck() {
        // #7781
        // Technically, editing a card with conditional fields can also cause this, but cloze cards are much more common

        Note n = addNoteUsingModelName("Cloze", "{{c1::Hello}} {{c2::World}}", "Extra");

        long did = addDeck("Testing");

        for (Card c : n.cards()) {
            c.setDid(did);
            c.flush();
        }

        assertThat("two cloze notes should be generated", n.numberOfCards(), is(2));

        // create card 3
        n.setField(0, n.getFields()[0] + "{{c3::third}}");

        n.flush();

        assertThat("A new card should be generated", n.numberOfCards(), is(3));

        assertThat("The new card should have the same did as the previous cards", n.cards().get(2).getDid(), is(did));
    }

    /*******************
     ** autogenerated from https://github.com/ankitects/anki/blob/2c73dcb2e547c44d9e02c20a00f3c52419dc277b/pylib/tests/test_cards.py *
     *******************/

    /*TODO
      @Test
      public void test_create_open(){
      (fd, path) = tempfile.mkstemp(suffix=".anki2", prefix="test_attachNew");
      try {
      os.close(fd);
      os.unlink(path);
      } catch (OSError) {
      }
      Collection col = aopen(path);
      // for open()
      String newPath = col.getPath();
      long newMod = col.getMod();
      col.close();

      // reopen
      col = aopen(newPath);
      assertEquals(newMod, col.getMod());
      col.close();

      // non-writeable dir
      if (isWin) {
      String dir = "c:\root.anki2";
      } else {
      String dir = "/attachroot.anki2";
      }
      assertException(Exception, lambda: aopen(dir));
      // reuse tmp file from before, test non-writeable file
      os.chmod(newPath, 0);
      assertException(Exception, lambda: aopen(newPath));
      os.chmod(newPath, 0o666);
      os.unlink(newPath);
      } */
    @Test
    public void test_noteAddDelete() {
        Collection col = getCol();
        // add a note
        Note note = col.newNote();
        note.setItem("Front", "one");
        note.setItem("Back", "two");
        int n = col.addNote(note);
        assertEquals(1, n);
        // test multiple cards - add another template
        Model m = col.getModels().current();
        Models mm = col.getModels();
        JSONObject t = Models.newTemplate("Reverse");
        t.put("qfmt", "{{Back}}");
        t.put("afmt", "{{Front}}");
        mm.addTemplateModChanged(m, t);
        mm.save(m, true); // todo: remove true which is not upstream
        assertEquals(2, col.cardCount());
        // creating new notes should use both cards
        note = col.newNote();
        note.setItem("Front", "three");
        note.setItem("Back", "four");
        n = col.addNote(note);
        assertEquals(2, n);
        assertEquals(4, col.cardCount());
        // check q/a generation
        Card c0 = note.cards().get(0);
        assertThat(c0.q(), containsString("three"));
        // it should not be a duplicate
        assertEquals(note.dupeOrEmpty(), Note.DupeOrEmpty.CORRECT);
        // now let's make a duplicate
        Note note2 = col.newNote();
        note2.setItem("Front", "one");
        note2.setItem("Back", "");
        assertNotEquals(note2.dupeOrEmpty(), Note.DupeOrEmpty.CORRECT);
        // empty first field should not be permitted either
        note2.setItem("Front", " ");
        assertNotEquals(note2.dupeOrEmpty(), Note.DupeOrEmpty.CORRECT);
    }


    @Test
    @Ignore("I don't understand this csum")
    public void test_fieldChecksum() {
        Collection col = getCol();
        Note note = col.newNote();
        note.setItem("Front", "new");
        note.setItem("Back", "new2");
        col.addNote(note);
        assertEquals(0xc2a6b03f, col.getDb().queryLongScalar("select csum from notes"));
        // changing the val should change the checksum
        note.setItem("Front", "newx");
        note.flush();
        assertEquals(0x302811ae, col.getDb().queryLongScalar("select csum from notes"));
    }


    @Test
    public void test_addDelTags() {
        Collection col = getCol();
        Note note = col.newNote();
        note.setItem("Front", "1");
        col.addNote(note);
        Note note2 = col.newNote();
        note2.setItem("Front", "2");
        col.addNote(note2);
        // adding for a given id
        col.getTags().bulkAdd(Collections.singletonList(note.getId()), "foo");
        note.load();
        note2.load();
        assertTrue(note.getTags().contains("foo"));
        assertFalse(note2.getTags().contains("foo"));
        // should be canonified
        col.getTags().bulkAdd(Collections.singletonList(note.getId()), "foo aaa");
        note.load();
        assertEquals("aaa", note.getTags().get(0));
        assertEquals(2, note.getTags().size());
    }


    @Test
    public void test_timestamps() {
        Collection col = getCol();
        int stdModelSize = StdModels.STD_MODELS.length;
        assertEquals(col.getModels().all().size(), stdModelSize);
        for (int i = 0; i < 100; i++) {
            StdModels.BASIC_MODEL.add(col);
        }
        assertEquals(col.getModels().all().size(), 100 + stdModelSize);
    }


    @Test
    @Ignore("Pending port of media search from Rust code")
    public void test_furigana() {
        Collection col = getCol();
        Models mm = col.getModels();
        Model m = mm.current();
        // filter should work
        m.getJSONArray("tmpls").getJSONObject(0).put("qfmt", "{{kana:Front}}");
        mm.save(m);
        Note n = col.newNote();
        n.setItem("Front", "foo[abc]");
        col.addNote(n);
        Card c = n.cards().get(0);
        assertTrue(c.q().endsWith("abc"));
        // and should avoid sound
        n.setItem("Front", "foo[sound:abc.mp3]");
        n.flush();
        String question = c.q(true);
        assertThat("Question «" + question + "» does not contains «anki:play».", question, containsString("anki:play"));
        // it shouldn't throw an error while people are editing
        m.getJSONArray("tmpls").getJSONObject(0).put("qfmt", "{{kana:}}");
        mm.save(m);
        c.q(true);
    }

    @Test
    public void test_filterToValidCards() {
        Collection col = getCol();
        long cid = addNoteUsingBasicModel("foo", "bar").firstCard().getId();
        assertEquals( new ArrayList<>(Collections.singleton(cid)), col.filterToValidCards(new long[]{cid, cid + 1}));
    }
}
