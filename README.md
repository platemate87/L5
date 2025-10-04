# l1j-en

l1j-en is an English-language server emulator for the South Korean MMORPG,
Lineage 1.  It targets feature-complete support for the final 2009 US client.

See the project documentation on the
[l1j-en project wiki](https://github.com/Sage-BR/L1J4Team/wiki) for general
project info, the client connector, FAQ, and the setup guide.

## Deployment notes

Before rolling out builds that include HP/MP gain history persistence, apply
the database migration in [`db/update_085.sql`](db/update_085.sql) so the
`characters` table includes the `HpGainHistory` and `MpGainHistory` columns.
Fresh installations should also run [`db/l1jdb_m10.sql`](db/l1jdb_m10.sql) to
ensure the schema matches the code expectations.

If you need any help, contact our (https://forum.4teambr.com/index.php?topic=39)

**l1j-en created**

Around Oct, 2008 the project was renamed to l1j-en to detach it from any
particular server.  Sometime in l1j-en's early life we began pulling in updates
from l1j-jp2 as well.  jp2 was actually located on l1j.org at that time.

In 2014 the project slowly transitioned to git (on GitHub), away from SVN and
Google Code. Github has since removed the repo that was located there.

From 2014 to 2018 the code base has entered many periods of dormancy.  Work began
in earnest again to bring up the zelgo server. Several old and new
members of the project have randomly come and gone over that time period.

**Move to l1j.org**

In 2018 we moved to l1j.org and worked on cleaning things up, optimizing things
and sticking to the original open source principles of the project.  Code
compatibility with other l1j projects was no longer considered a priority,
allowing us to plug all known security holes that would plague high population
public servers.

**Last US version compatibility complete**

As of late 2019, the milestone 10 DB build was published, along with new build
scripts that work with Java 9 and higher (Java <8 support has been deprecated
accordingly).  We generally recommend running it on JDK 11 LTS, but others >8
should work.  With the majority of major issues addressed, the development team
considers this project in a stable state and fully usable for classic client
servers.

**Current status**

As of 2020-07-04, this repository has been moved back to GitHub, with any legal
concerns addressed (see legal section).  We've also funded a legal retainer
agreement should the need arise.  In the unlikely event this repo is moved
again, an announcement on the project's group forum will be posted.

Along with the above, we have also started a branch of this codebase that is
compatible with the feature set of 3.63, due to much popular demand (mainly
enabling the higher resolution clients).  This is already in a usable state,
and we are closing the gap on new features to get it of comparable quality to
what's in the master branch.  Do a checkout against branch `363` to use this.

### Legal

No one behind this project offers an opinion on the legality of running a
private server.  This project only covers just the codebase and ancillary
documentation you see here.  l1j-en itself is devoid of any information gleaned
directly from the client.  This is a "clean room" implementation of a server
compatible with various clients, all implementing long abandoned versions of
game feature sets.

None of the contributors to this project are or have in the past been customers
of the original creators of Lineage, and are therefore not beholden to any
EULAs a customer must agree to.  Despite this, we intentionally to not touch
the official client, only making use of custom-built headless clients and
therefore run no proprietary software in the development process.  A side goal
of this team is to create various clients that can be used with this server,
and these are the only methods of play that are officially endorsed here.

As such, we will not accept contributions that potentially violate
copyright/trademark law.

### Contributing

Report issues and suggest features and improvements on the
[l1j.org issue tracker](https://github.com/Sage-BR/L1J4Team/issues). Discussions and questions
should go to the mailing list and IRC channel.

Pull requests are always welcome!  For those migrating to Git from SVN, see the
[DEVGUIDE](DEVGUIDE.md).

Those interested in joining the development effort as a team member should feel
free to let us know, preferably on the IRC channel.  We always need additional
help with programming, Lineage 1 domain knowledge (particularly with newer
game content), and documentation.

### License

Distributed under the GNU General Public License, version 2.
